package com.hongseob.openclass_ap.enrollment.request;

import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 큐 테이블 스키마 통합 테스트 — AC-ENR-004 (M1).
 *
 * <p>애플리케이션 계층을 우회한 {@link JdbcTemplate} 직접 INSERT로 DB CHECK
 * 제약의 실제 동작을 검증한다 — 실제 PostgreSQL(Testcontainers)에서만 의미가
 * 있다(plan.md §D, research.md §6).</p>
 */
@SpringBootTest
class EnrollmentQueueSchemaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final long COURSE_ID = 1L;
    private static final long MEMBER_ID = 1L;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM enrollment_request");
    }

    private void insertRawRequest(String requestType, String state, String result) {
        jdbcTemplate.update(
                "INSERT INTO enrollment_request (member_id, course_id, request_type, state, result, requested_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                MEMBER_ID, COURSE_ID, requestType, state, result, LocalDateTime.now());
    }

    // AC-ENR-004
    @Test
    void request_type이_도메인_밖_값이면_DB_제약이_거부하고_3종_값은_허용한다() {
        assertThatThrownBy(() -> insertRawRequest("PROMOTE", "PENDING", null))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatCode(() -> insertRawRequest("ENROLL", "PENDING", null)).doesNotThrowAnyException();
        assertThatCode(() -> insertRawRequest("CANCEL", "PENDING", null)).doesNotThrowAnyException();
        assertThatCode(() -> insertRawRequest("CAPACITY_INCREASE", "PENDING", null)).doesNotThrowAnyException();
    }

    // spec.md §A.4.1 상태/결과 도메인 — CHECK 제약이 domain 밖 값과 상태/결과
    // 불일치(state='PENDING'인데 result가 채워짐)를 함께 거부하는지 확인한다.
    // M1의 스키마 확정 범위이며, AC-ENR-004의 "그 외의 값을 저장하지 않는다"에
    // 포함되는 인접 검증이다.
    @Test
    void state와_result_도메인_밖_값이거나_상태_결과가_불일치하면_DB_제약이_거부한다() {
        assertThatThrownBy(() -> insertRawRequest("ENROLL", "PROCESSING", null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRawRequest("ENROLL", "DONE", "INVALID_RESULT"))
                .isInstanceOf(DataIntegrityViolationException.class);
        // state=PENDING인데 result가 채워져 있으면 §A.4.1 전이표 위반
        assertThatThrownBy(() -> insertRawRequest("ENROLL", "PENDING", "SUCCESS"))
                .isInstanceOf(DataIntegrityViolationException.class);
        // state=DONE인데 result가 NULL이면 마찬가지로 위반
        assertThatThrownBy(() -> insertRawRequest("ENROLL", "DONE", null))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatCode(() -> insertRawRequest("ENROLL", "DONE", "SUCCESS")).doesNotThrowAnyException();
    }
}
