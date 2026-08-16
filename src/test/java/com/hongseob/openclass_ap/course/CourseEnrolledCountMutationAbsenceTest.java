package com.hongseob.openclass_ap.course;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-CRS-004(ii) — 프로덕션 소스에 {@code enrolled_count}를 변경하는 코드 경로가
 * 없음을 정적 검색으로 검증한다.
 *
 * <p>ArchUnit은 도입하지 않는다 — 이 AC가 검증할 명제는 "변경 경로가 0건"이라는
 * 부재 명제이며 grep 수준의 정적 검색으로 충분히 관찰된다. 패키지 스코프 규칙이
 * 아니므로 ArchUnit이 실제로 필요한 쪽이 아니다 (plan.md HISTORY 0.1.2 결정 근거
 * D3, plan.md §D "신규 인프라 추가 금지").</p>
 *
 * <p>{@code Course.java}(엔티티 필드 선언·getter·정적 팩토리의 최초 0 설정)는
 * 검색 대상에서 제외한다 — AC-CRS-004가 명시한 "엔티티 필드 선언과 JPA 읽기
 * 매핑은 제외" 범위이며, 정적 팩토리 안에서 {@code enrolled_count}를 0으로 최초
 * 설정하는 것은 REQ-ADM-003이 요구하는 "생성" 동작 자체다 — 기존 값을 "변경"하는
 * 것이 아니다 (plan.md §C.4.1-2 구조적 뒷받침).</p>
 */
class CourseEnrolledCountMutationAbsenceTest {

    private static final Path SRC_MAIN = Path.of("src/main/java");

    @Test
    void 프로덕션_소스에서_Course_엔티티_외에는_enrolled_count_참조가_전혀_없다() throws IOException {
        List<String> offendingLines;
        try (Stream<Path> paths = Files.walk(SRC_MAIN)) {
            offendingLines = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("Course.java"))
                    .flatMap(this::linesMatching)
                    .toList();
        }

        assertThat(offendingLines).isEmpty();
    }

    private Stream<String> linesMatching(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .filter(line -> line.contains("enrolledCount") || line.contains("enrolled_count"))
                    .map(line -> path + " -> " + line.trim());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
