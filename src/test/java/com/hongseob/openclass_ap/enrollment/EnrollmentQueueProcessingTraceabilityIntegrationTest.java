package com.hongseob.openclass_ap.enrollment;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 큐 처리 추적성 통합 테스트(M6, REQ-NFR-004, AC-ENR-047). 요청 1건이
 * {@link EnrollmentRequestProcessor#processOne}을 거쳐 처리될 때, 그 요청
 * 식별자를 포함한 처리 시작·종료·결과 로그가 실제로 기록되는지 확인한다.
 * {@code SensitiveLogIntegrationTest}(M4)와 동일한 로그 캡처 패턴({@code
 * ListAppender})을 재사용한다 — 이번에는 루트 로거 대신 {@link
 * EnrollmentRequestProcessor}의 로거만 첨부해 다른 컴포넌트의 로그 잡음을
 * 배제한다.
 */
@SpringBootTest
class EnrollmentQueueProcessingTraceabilityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private EnrollmentQueueWorker worker;

    // AC-ENR-047
    @Test
    void 요청_1건_처리시_요청식별자를_포함한_시작_종료_결과_로그가_기록된다() {
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        Member member = memberRepository.save(Member.createMember("trace@example.com", "hash", "학생"));
        Course course = courseRepository.save(Course.create("추적성강좌", "설명", 5,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30)));

        Long requestId = receiptService.receiveEnrollment(member.getId(), course.getId());

        Logger processorLogger = (Logger) LoggerFactory.getLogger(EnrollmentRequestProcessor.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        processorLogger.addAppender(appender);

        try {
            worker.drainQueue();
        } finally {
            processorLogger.detachAppender(appender);
        }

        List<String> logMessages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        String requestIdToken = "requestId=" + requestId;
        assertThat(logMessages)
                .anyMatch(message -> message.contains("처리 시작") && message.contains(requestIdToken));
        assertThat(logMessages)
                .anyMatch(message -> message.contains("처리 종료") && message.contains(requestIdToken)
                        && message.contains("result="));
    }
}
