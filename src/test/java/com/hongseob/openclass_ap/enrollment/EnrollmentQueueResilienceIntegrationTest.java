package com.hongseob.openclass_ap.enrollment;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequest;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.request.RequestState;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentFailureInjector;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor;
import com.hongseob.openclass_ap.enrollment.worker.fixture.EnrollmentFailureInjectorTestConfig;
import com.hongseob.openclass_ap.enrollment.worker.fixture.EnrollmentFailureInjectorTestConfig.ControllableFailureInjector;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실패 격리·처리 원자성·재처리 멱등성 — AC-ENR-016(단일 요청 실패가 큐를
 * 멈추지 않음), AC-ENR-017(처리 원자성 및 실패 기록 보존),
 * AC-ENR-018(재처리 멱등성 및 상태 단방향성) (M2).
 *
 * <p>{@link EnrollmentFailureInjectorTestConfig}로 프로덕션 기본 무동작
 * 구현을 테스트 전용 제어 가능 구현으로 교체해, "확정 행 INSERT 이후,
 * 커밋 이전" 예외 시나리오를 결정적으로 재현한다(design.md §4.2).</p>
 */
@SpringBootTest
@Import(EnrollmentFailureInjectorTestConfig.class)
class EnrollmentQueueResilienceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private EnrollmentQueueWorker worker;

    @Autowired
    private EnrollmentRequestProcessor processor;

    @Autowired
    private EnrollmentRequestRepository requestRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EnrollmentFailureInjector failureInjector;

    private ControllableFailureInjector controllableInjector;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
        controllableInjector = (ControllableFailureInjector) failureInjector;
        controllableInjector.reset();
    }

    private Long createCourse(int capacity) {
        return courseRepository.save(Course.create("복원력테스트강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
    }

    private Long createMember(String email) {
        return memberRepository.save(Member.createMember(email, "hash", "회원")).getId();
    }

    // AC-ENR-016 + AC-ENR-017
    @Test
    void 두번째_요청의_확정_INSERT_직후_예외가_발생해도_나머지_요청은_정상_종결되고_실패한_요청의_도메인_변경만_롤백되며_FAILED로_기록된다() {
        Long courseId = createCourse(5);
        Long m1 = createMember("resil1@example.com");
        Long m2 = createMember("resil2@example.com");
        Long m3 = createMember("resil3@example.com");

        Long r1 = receiptService.receiveEnrollment(m1, courseId);
        Long r2 = receiptService.receiveEnrollment(m2, courseId);
        Long r3 = receiptService.receiveEnrollment(m3, courseId);

        controllableInjector.failNextFor(r2);

        worker.drainQueue();

        // AC-ENR-016 — 실패한 두 번째 요청이 나머지 요청 처리를 막지 않는다.
        assertThat(requestRepository.findById(r1).orElseThrow().getResult())
                .isEqualTo(RequestResult.SUCCESS);
        assertThat(requestRepository.findById(r3).orElseThrow().getResult())
                .as("세 번째 요청이 PENDING으로 남지 않고 정상 종단되어야 한다")
                .isEqualTo(RequestResult.SUCCESS);

        // AC-ENR-017 — 실패한 요청의 상태 전이(FAILED)는 남지만 도메인 변경은 롤백된다.
        EnrollmentRequest r2Row = requestRepository.findById(r2).orElseThrow();
        assertThat(r2Row.getState())
                .as("PENDING으로 남아 무한 재시도되지 않고 DONE으로 종결되어야 한다")
                .isEqualTo(RequestState.DONE);
        assertThat(r2Row.getResult()).isEqualTo(RequestResult.FAILED);

        assertThat(enrollmentRepository.count())
                .as("실패한 요청의 확정 INSERT는 롤백되어야 한다 — m1·m3 두 건만 남는다")
                .isEqualTo(2);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                .as("실패한 요청의 enrolled_count 증가도 함께 롤백되어야 한다")
                .isEqualTo(2);
        assertThat(enrollmentRepository.findAll())
                .extracting(Enrollment::getMemberId)
                .containsExactlyInAnyOrder(m1, m3);
    }

    // AC-ENR-018
    @Test
    void 이미_DONE인_요청을_강제로_재처리해도_도메인이_변하지_않고_결과도_유지된다() {
        Long courseId = createCourse(5);
        Long memberId = createMember("idempotent@example.com");
        Long requestId = receiptService.receiveEnrollment(memberId, courseId);
        worker.drainQueue();
        assertThat(requestRepository.findById(requestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.SUCCESS);

        // 이미 state='DONE'인 행에 워커 처리 루프(processOne)를 강제로 1회 더
        // 실행한다 — findPendingForUpdateSkipLocked가 state='PENDING' 조건으로
        // 조회하므로 이미 DONE인 행은 조회되지 않고 조용히 무동작한다
        // (REQ-WRK-011).
        processor.processOne(requestId);

        assertThat(enrollmentRepository.count())
                .as("재처리로 확정 행이 추가 생성되지 않아야 한다").isEqualTo(1);
        assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount()).isEqualTo(1);
        assertThat(requestRepository.findById(requestId).orElseThrow().getResult())
                .isEqualTo(RequestResult.SUCCESS);

        // DONE → PENDING 역전이를 수행하는 프로덕션 코드 경로 자체가 존재하지
        // 않는다 — EnrollmentRequest는 state를 PENDING으로 되돌리는 세터/메서드를
        // 어떤 이름으로도 정의하지 않는다(markDone()만이 state를 변경하며 항상
        // DONE으로 설정한다). 이 사실은 소스 검색으로 별도 확인한다(검증 방법:
        // acceptance.md AC-ENR-018 "통합 테스트 + 소스 검색").
    }

    // AC-ENR-059 — M8 Step A: 실패 예외의 상세 기록 (레벨 + requestId + 예외
    // 타입 + 예외 메시지 + 스택 트레이스가 모두 확인되고, 동시에 AC-ENR-016/017의
    // 기존 동작(다른 요청 정상 종단 + result='FAILED' 보존)이 유지된다).
    @Test
    void 두번째_요청_실패시_WARN_이상_수준으로_requestId_예외타입_예외메시지_스택트레이스가_모두_기록되고_AC_ENR_016_017_동작은_유지된다() {
        Logger workerLogger = (Logger) LoggerFactory.getLogger(EnrollmentQueueWorker.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        workerLogger.addAppender(appender);

        try {
            Long courseId = createCourse(5);
            Long m1 = createMember("diag1@example.com");
            Long m2 = createMember("diag2@example.com");
            Long m3 = createMember("diag3@example.com");

            Long r1 = receiptService.receiveEnrollment(m1, courseId);
            Long r2 = receiptService.receiveEnrollment(m2, courseId);
            Long r3 = receiptService.receiveEnrollment(m3, courseId);

            controllableInjector.failNextFor(r2);

            worker.drainQueue();

            // AC-ENR-059 — 로그 이벤트 중 최소 1건이 5개 요소를 모두 만족한다.
            List<ILoggingEvent> matching = appender.list.stream()
                    .filter(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                    .filter(event -> event.getFormattedMessage().contains("requestId=" + r2))
                    .toList();

            assertThat(matching)
                    .as("requestId=%s 를 포함하는 WARN 이상 로그 이벤트가 최소 1건 있어야 한다", r2)
                    .isNotEmpty();

            ILoggingEvent event = matching.get(0);
            assertThat(event.getLevel().isGreaterOrEqual(Level.WARN))
                    .as("로그 수준이 WARN 이상이어야 한다").isTrue();
            assertThat(event.getThrowableProxy())
                    .as("스택 트레이스(ThrowableProxy)가 첨부되어 있어야 한다").isNotNull();
            assertThat(event.getThrowableProxy().getClassName())
                    .as("예외 타입이 주입된 예외(IllegalStateException)와 일치해야 한다")
                    .isEqualTo(IllegalStateException.class.getName());
            assertThat(event.getThrowableProxy().getMessage())
                    .as("예외 메시지가 주입 시 사용한 메시지를 포함해야 한다")
                    .contains("AC-ENR-016/017 테스트 전용 강제 실패 주입: requestId=" + r2);
            assertThat(event.getThrowableProxy().getStackTraceElementProxyArray().length)
                    .as("스택 트레이스 프레임이 1개 이상이어야 한다")
                    .isGreaterThan(0);

            // AC-ENR-059 "또한" 절 — AC-ENR-016/017의 기존 동작이 그대로 유지된다.
            assertThat(requestRepository.findById(r1).orElseThrow().getResult())
                    .isEqualTo(RequestResult.SUCCESS);
            assertThat(requestRepository.findById(r3).orElseThrow().getResult())
                    .isEqualTo(RequestResult.SUCCESS);
            EnrollmentRequest r2Row = requestRepository.findById(r2).orElseThrow();
            assertThat(r2Row.getState()).isEqualTo(RequestState.DONE);
            assertThat(r2Row.getResult()).isEqualTo(RequestResult.FAILED);
            assertThat(enrollmentRepository.count()).isEqualTo(2);
            assertThat(courseRepository.findById(courseId).orElseThrow().getEnrolledCount())
                    .isEqualTo(2);
        } finally {
            workerLogger.detachAppender(appender);
        }
    }
}
