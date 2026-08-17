package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DB 제약이 최종 방어선으로 실제 동작하는지 애플리케이션 계층 우회로 검증한다
 * — AC-ENR-015(중복 확정 DB 제약), AC-ENR-021(정원 초과 DB 제약) (M2).
 */
@SpringBootTest
class EnrollmentDbConstraintBackstopIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @BeforeEach
    void cleanUp() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private Course createCourse(int capacity) {
        return courseRepository.save(Course.create("제약테스트강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30)));
    }

    // AC-ENR-015
    @Test
    void 동일_강좌_동일_회원의_중복_확정_행_직접_INSERT는_DB_제약으로_거부된다() {
        Course course = createCourse(5);
        Member member = memberRepository.save(Member.createMember("constraint-dup@example.com", "hash", "회원"));

        enrollmentRepository.save(Enrollment.create(member.getId(), course.getId()));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO enrollment (member_id, course_id, status, enrolled_at) VALUES (?, ?, 'ENROLLED', ?)",
                member.getId(), course.getId(), LocalDateTime.now()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // AC-ENR-021
    @Test
    void 확정_인원이_정원과_같을_때_enrolled_count를_직접_증가시키는_UPDATE는_DB_제약으로_거부된다() {
        Course course = createCourse(5);
        jdbcTemplate.update("UPDATE course SET enrolled_count = 5 WHERE id = ?", course.getId());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE course SET enrolled_count = 6 WHERE id = ?", course.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
