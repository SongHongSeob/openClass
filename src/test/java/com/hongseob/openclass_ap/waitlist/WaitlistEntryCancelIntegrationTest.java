package com.hongseob.openclass_ap.waitlist;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.EnrollmentRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 대기명단 취소 통합 테스트 — AC-ENR-033(취소 시 상대 순서 보존),
 * AC-ENR-034(타인 대기 취소 차단, API 계층) (M4).
 */
@SpringBootTest
@AutoConfigureMockMvc
class WaitlistEntryCancelIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WaitlistService waitlistService;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private EnrollmentQueueWorker worker;

    @Autowired
    private EnrollmentRequestRepository requestRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        waitlistEntryRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private Long createCourse(int capacity) {
        return courseRepository.save(Course.create("대기취소강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30))).getId();
    }

    private Long createMember(String email) {
        return memberRepository.save(Member.createMember(email, "hash", "회원")).getId();
    }

    private Long confirmedEnrollmentIdOf(Long memberId, Long courseId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM enrollment WHERE member_id = ? AND course_id = ? AND status = 'ENROLLED'",
                Long.class, memberId, courseId);
    }

    // AC-ENR-033
    @Test
    void 대기_취소는_취소한_항목만_전이하고_뒤_순번의_상대_순서를_보존하며_다음_승격_대상은_먼저_취소하지_않은_대기자다() {
        Long courseId = createCourse(1);
        Long filler = createMember("order-preserve-filler@example.com");
        Long c = createMember("order-preserve-c@example.com");
        Long d = createMember("order-preserve-d@example.com");
        Long e = createMember("order-preserve-e@example.com");

        receiptService.receiveEnrollment(filler, courseId);
        receiptService.receiveEnrollment(c, courseId);
        receiptService.receiveEnrollment(d, courseId);
        receiptService.receiveEnrollment(e, courseId);
        worker.drainQueue();

        WaitlistEntry cEntry = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(c, courseId, WaitlistStatus.WAITING).orElseThrow();
        WaitlistEntry dEntry = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(d, courseId, WaitlistStatus.WAITING).orElseThrow();
        WaitlistEntry eEntry = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(e, courseId, WaitlistStatus.WAITING).orElseThrow();
        assertThat(cEntry.getPosition()).isEqualTo(1L);
        assertThat(dEntry.getPosition()).isEqualTo(2L);
        assertThat(eEntry.getPosition()).isEqualTo(3L);

        // D가 자신의 대기를 취소한다.
        waitlistService.cancel(dEntry.getId(), d);

        WaitlistEntry cAfterDCancel = waitlistEntryRepository.findById(cEntry.getId()).orElseThrow();
        WaitlistEntry dAfterCancel = waitlistEntryRepository.findById(dEntry.getId()).orElseThrow();
        WaitlistEntry eAfterDCancel = waitlistEntryRepository.findById(eEntry.getId()).orElseThrow();
        assertThat(dAfterCancel.getStatus()).isEqualTo(WaitlistStatus.CANCELLED);
        assertThat(cAfterDCancel.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(cAfterDCancel.getPosition()).isEqualTo(1L);
        assertThat(eAfterDCancel.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(eAfterDCancel.getPosition()).isEqualTo(3L);

        // 확정자 1명이 취소되어 승격이 일어나면 대상은 C다(D를 건너뛰지 않고
        // 애초에 활성 대기자가 아니므로 대상이 아니다).
        Long fillerEnrollmentId = confirmedEnrollmentIdOf(filler, courseId);
        receiptService.receiveCancel(filler, fillerEnrollmentId);
        worker.drainQueue();

        assertThat(waitlistEntryRepository.findById(cEntry.getId()).orElseThrow().getStatus())
                .isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(waitlistEntryRepository.findById(dEntry.getId()).orElseThrow().getStatus())
                .as("D는 여전히 CANCELLED로 남아 있어야 한다 — 승격 대상이 아니다")
                .isEqualTo(WaitlistStatus.CANCELLED);
        assertThat(waitlistEntryRepository.findById(eEntry.getId()).orElseThrow().getStatus())
                .as("E는 C 승격 이후에도 여전히 대기 중이어야 한다 — C를 앞지르지 않는다")
                .isEqualTo(WaitlistStatus.WAITING);
    }

    // AC-ENR-034
    @Test
    void 타인의_대기_항목을_취소하면_403_또는_404이고_소유자의_대기_상태와_순번이_불변이다() throws Exception {
        Long courseId = createCourse(1);
        Long filler = createMember("wl-own-filler@example.com");
        Long owner = createMember("wl-own-owner@example.com");
        Member intruder = memberRepository.save(Member.createMember("wl-own-intruder@example.com", "hash", "F"));
        String intruderToken = jwtTokenProvider.generateToken(intruder);

        receiptService.receiveEnrollment(filler, courseId);
        receiptService.receiveEnrollment(owner, courseId);
        worker.drainQueue();

        WaitlistEntry ownerEntry = waitlistEntryRepository
                .findByMemberIdAndCourseIdAndStatus(owner, courseId, WaitlistStatus.WAITING).orElseThrow();

        int status = mockMvc.perform(delete("/api/waitlist-entries/" + ownerEntry.getId())
                        .header("Authorization", "Bearer " + intruderToken))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(403, 404);

        WaitlistEntry unchanged = waitlistEntryRepository.findById(ownerEntry.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(unchanged.getPosition()).isEqualTo(ownerEntry.getPosition());

        // 소유자 본인은 정상적으로 취소할 수 있다.
        String ownerToken = jwtTokenProvider.generateToken(memberRepository.findAll().stream()
                .filter(m -> m.getId().equals(owner)).findFirst().orElseThrow());
        mockMvc.perform(delete("/api/waitlist-entries/" + ownerEntry.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        assertThat(waitlistEntryRepository.findById(ownerEntry.getId()).orElseThrow().getStatus())
                .isEqualTo(WaitlistStatus.CANCELLED);
    }
}
