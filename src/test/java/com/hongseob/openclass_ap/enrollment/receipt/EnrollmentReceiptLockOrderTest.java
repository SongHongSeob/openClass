package com.hongseob.openclass_ap.enrollment.receipt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-ENR-005 — 접수 잠금 획득이 큐 행 INSERT보다 소스에서 선행함을 정적으로
 * 검증한다(구조적 검증). ast-grep 수준의 순서 검사를 텍스트 인덱스 비교로
 * 수행한다 — 실제 런타임 순서는 AC-ENR-006(핵심 시나리오)·AC-ENR-007(대조군)이
 * 검증한다. 이 클래스는 소스 텍스트 순서가 고정되어 있음을 확인한다.
 *
 * <p>{@code @MX:ANCHOR}/{@code @MX:REASON} 주석 존재도 함께 확인한다(순서
 * 의존성이 코드만 봐서는 드러나지 않으므로 design.md §3이 요구하는 불변
 * 계약 주석).</p>
 */
class EnrollmentReceiptLockOrderTest {

    private static final Path SERVICE_FILE = Path.of(
            "src/main/java/com/hongseob/openclass_ap/enrollment/receipt/EnrollmentReceiptService.java");

    @Test
    void 접수_잠금_획득_호출이_큐_행_저장_호출보다_소스에서_먼저_나타난다() throws IOException {
        String source = Files.readString(SERVICE_FILE);

        int lockIndex = source.indexOf("pg_advisory_xact_lock");
        int saveIndex = source.indexOf("requestRepository.save(");

        assertThat(lockIndex).as("pg_advisory_xact_lock 호출이 존재해야 한다").isPositive();
        assertThat(saveIndex).as("requestRepository.save 호출이 존재해야 한다").isPositive();
        assertThat(lockIndex).as("잠금 획득이 큐 행 저장보다 먼저 나타나야 한다").isLessThan(saveIndex);
    }

    @Test
    void 접수_잠금_지점에_MX_ANCHOR와_REASON_주석이_존재한다() throws IOException {
        String source = Files.readString(SERVICE_FILE);

        assertThat(source).contains("@MX:ANCHOR");
        assertThat(source).contains("@MX:REASON");

        int anchorIndex = source.indexOf("@MX:ANCHOR");
        int saveIndex = source.indexOf("requestRepository.save(");
        assertThat(anchorIndex)
                .as("@MX:ANCHOR 주석은 큐 행 저장 호출 바로 앞의 순서-불변 지점에 있어야 한다")
                .isLessThan(saveIndex);
    }
}
