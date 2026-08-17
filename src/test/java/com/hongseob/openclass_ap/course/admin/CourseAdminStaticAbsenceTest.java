package com.hongseob.openclass_ap.course.admin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-ADM-005(ii) / AC-ADM-007 — 프로덕션 소스 정적 검색.
 *
 * <p>AC-ADM-005(ii)는 대기명단·승격 관련 식별자({@code waitlist}, {@code promote},
 * {@code 승격})가 이 SPEC 범위에 전혀 존재하지 않음을 검증한다 — 승격 로직은
 * {@code SPEC-ENROLLMENT-001}의 책임이며 이 SPEC의 산출물로 관찰 가능한 것만
 * 단언한다(acceptance.md 검증 범위 주석).</p>
 *
 * <p>AC-ADM-007은 {@code course}에 대한 물리 삭제({@code .delete(}/{@code .remove(})
 * 호출 지점이 없음을 검증한다 — "삭제" API는 내부적으로 {@code close()}와 동일한
 * 마감 전이만 수행한다(plan.md §C.1 설계 판단 4, REQ-ADM-008 / INV-CRS-004).
 * {@code CourseRepository}가 {@link org.springframework.data.jpa.repository.JpaRepository}를
 * 상속하며 상속받은 {@code delete}류 메서드 선언 자체는 이 프로젝트 소스에
 * 나타나지 않으므로 호출부만 검색하면 충분하다.</p>
 *
 * <p>검색 범위는 {@code course} 패키지로 한정한다 — "이 SPEC의 산출물로 관찰
 * 가능한 것만 단언한다"는 위 검증 범위 주석을 그대로 반영한 것이다.
 * {@code SPEC-ENROLLMENT-001}이 M4/M5에서 정당하게 추가한 {@code enrollment.waitlist}
 * 패키지·{@code promoteNextEligible} 헬퍼는 이 SPEC(SPEC-COURSE-001)의 산출물이
 * 아니므로 이 단언의 대상이 아니다 — 원래 {@code src/main/java} 전체를 스캔하던
 * 구현은 SPEC-ENROLLMENT-001이 아직 존재하지 않던 시점의 우연한 등가였을 뿐,
 * 이 AC가 실제로 요구하는 범위가 아니었다.</p>
 */
class CourseAdminStaticAbsenceTest {

    private static final Path SRC_MAIN = Path.of("src/main/java/com/hongseob/openclass_ap/course");
    private static final List<String> WAITLIST_KEYWORDS = List.of("waitlist", "promote", "승격");
    private static final List<String> DELETE_CALL_PATTERNS = List.of(".delete(", ".remove(");

    @Test
    void 프로덕션_소스에_대기명단_승격_관련_식별자가_전혀_없다() throws IOException {
        List<String> offendingLines = scan(line -> {
            String lower = line.toLowerCase();
            return WAITLIST_KEYWORDS.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase()) || line.contains(keyword));
        });

        assertThat(offendingLines).isEmpty();
    }

    @Test
    void 프로덕션_소스에_course에_대한_물리_삭제_호출이_없다() throws IOException {
        List<String> offendingLines = scan(line -> DELETE_CALL_PATTERNS.stream().anyMatch(line::contains));

        assertThat(offendingLines).isEmpty();
    }

    private boolean isCommentLine(String trimmed) {
        return trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*");
    }

    private List<String> scan(java.util.function.Predicate<String> lineMatcher) throws IOException {
        try (Stream<Path> paths = Files.walk(SRC_MAIN)) {
            return paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> linesMatching(path, lineMatcher))
                    .toList();
        }
    }

    private Stream<String> linesMatching(Path path, java.util.function.Predicate<String> lineMatcher) {
        try {
            return Files.readAllLines(path).stream()
                    .filter(lineMatcher)
                    .filter(line -> !isCommentLine(line.trim()))
                    .map(line -> path + " -> " + line.trim());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
