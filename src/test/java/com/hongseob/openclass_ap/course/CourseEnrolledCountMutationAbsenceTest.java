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
 * 없음을 정적 검색으로 검증한다. acceptance.md 원문은 "정적 검색은 설정·증가·감소·
 * 0으로의 초기화를 모두 대상으로 한다"고 범위를 명시한다 — 즉 이 AC가 금지하는
 * 것은 <b>변경</b>이지 <b>참조(읽기)</b> 자체가 아니다.
 *
 * <p>ArchUnit은 도입하지 않는다 — 이 AC가 검증할 명제는 "변경 경로가 0건"이라는
 * 부재 명제이며 grep 수준의 정적 검색으로 충분히 관찰된다. 패키지 스코프 규칙이
 * 아니므로 ArchUnit이 실제로 필요한 쪽이 아니다 (plan.md HISTORY 0.1.2 결정 근거
 * D3, plan.md §D "신규 인프라 추가 금지").</p>
 *
 * <p>{@code Course.java}(엔티티 필드 선언·getter·정적 팩토리의 최초 0 설정),
 * {@code course/dto/CourseResponse.java}(잔여 정원 계산을 위한 읽기 전용 record
 * 필드 — record는 세터가 없어 구조적으로 변경 불가능하며, plan.md §C.1 설계
 * 판단 2가 요구하는 "DTO 매핑 계층에서 조회 시 계산" 자체다), 그리고 M3에서 추가된
 * {@code common/exception/CapacityBelowEnrollmentException.java}(409 예외 메시지에
 * 확정 인원 값을 담는 생성자 파라미터 — {@code Course} 엔티티를 전혀 참조하지
 * 않는 읽기 전용 값 전달이며, {@code CourseResponse}와 동일한 성격이다)는 검색
 * 대상에서 제외한다 — AC-CRS-004가 명시한 "엔티티 필드 선언과 JPA 읽기 매핑은
 * 제외" 범위와 동일한 성격이다. 주석(Javadoc)도 코드가 아니므로 제외한다 — 주석은
 * 그 자체로 필드를 변경할 수 없다.</p>
 */
class CourseEnrolledCountMutationAbsenceTest {

    private static final Path SRC_MAIN = Path.of("src/main/java");
    private static final List<String> EXCLUDED_FILES =
            List.of("Course.java", "CourseResponse.java", "CapacityBelowEnrollmentException.java");

    @Test
    void 프로덕션_소스에서_Course_엔티티와_읽기_전용_DTO_외에는_enrolled_count_참조가_전혀_없다() throws IOException {
        List<String> offendingLines;
        try (Stream<Path> paths = Files.walk(SRC_MAIN)) {
            offendingLines = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !EXCLUDED_FILES.contains(path.getFileName().toString()))
                    .flatMap(this::linesMatching)
                    .toList();
        }

        assertThat(offendingLines).isEmpty();
    }

    private boolean isCommentLine(String trimmed) {
        return trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*");
    }

    private Stream<String> linesMatching(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .filter(line -> line.contains("enrolledCount") || line.contains("enrolled_count"))
                    .filter(line -> !isCommentLine(line.trim()))
                    .map(line -> path + " -> " + line.trim());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
