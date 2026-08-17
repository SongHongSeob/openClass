package com.hongseob.openclass_ap.enrollment.receipt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * 메서드별 순서 검사(SPEC-ENROLLMENT-001 sync-audit F2) — {@code indexOf}로
     * 최초 출현만 비교하면 3개 메서드(receiveEnrollment/receiveCancel/
     * receiveCapacityIncrease) 중 첫 번째만 검증되고 나머지 2개의 회귀나 잠금
     * 없이 저장하는 4번째 메서드 추가를 잡지 못한다. 모든 출현을 순서대로
     * 짝지어 각 쌍이 [잠금 → 저장] 순서인지 확인한다.
     */
    @Test
    void 접수_잠금_획득_호출이_큐_행_저장_호출보다_소스에서_먼저_나타난다() throws IOException {
        String source = Files.readString(SERVICE_FILE);

        // 정확한 호출부 패턴("...lock(?, ?)")만 매칭한다 — 단순 "pg_advisory_xact_lock"
        // 문자열은 클래스/메서드 Javadoc과 인라인 주석에도 설명 목적으로 등장하므로
        // (예: 33·75행) 그대로 세면 실제 호출 3건보다 많이 잡힌다.
        List<Integer> lockIndices = allIndicesOf(source, "pg_advisory_xact_lock(?, ?)");
        List<Integer> saveIndices = allIndicesOf(source, "requestRepository.save(");

        assertThat(lockIndices).as("pg_advisory_xact_lock 호출이 1건 이상 존재해야 한다").isNotEmpty();
        assertThat(saveIndices)
                .as("requestRepository.save 호출 횟수와 pg_advisory_xact_lock 호출 횟수가 일치해야 한다"
                        + " — 잠금 없이 저장하는 경로가 없음을 보장한다")
                .hasSameSizeAs(lockIndices);

        for (int i = 0; i < saveIndices.size(); i++) {
            assertThat(lockIndices.get(i))
                    .as("%d번째 저장 호출(%d) 이전에 그에 대응하는 잠금 획득이 있어야 한다", i + 1, saveIndices.get(i))
                    .isLessThan(saveIndices.get(i));
        }
    }

    private static List<Integer> allIndicesOf(String source, String needle) {
        List<Integer> indices = new ArrayList<>();
        int from = 0;
        while (true) {
            int idx = source.indexOf(needle, from);
            if (idx < 0) {
                break;
            }
            indices.add(idx);
            from = idx + needle.length();
        }
        return indices;
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
