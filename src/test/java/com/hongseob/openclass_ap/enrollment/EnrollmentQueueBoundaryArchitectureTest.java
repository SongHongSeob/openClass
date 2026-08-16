package com.hongseob.openclass_ap.enrollment;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * AC-ENR-005 — 잠금 없이 큐 행을 INSERT하는 프로덕션 경로가 0건임을 패키지
 * 구조로도 보장한다. {@code EnrollmentRequestRepository}(접수 잠금을 거친 뒤
 * 저장하는 {@link com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService}가
 * 사용하는 저장소)를 참조할 수 있는 패키지를 접수(receipt)·워커(worker)·큐
 * 엔티티 자신(request) 패키지로 한정한다 — 다른 패키지가 이 저장소에 직접
 * 접근해 잠금을 우회한 채 큐 행을 만드는 경로를 구조적으로 차단한다.
 *
 * <p><b>M1 범위 고지</b>: 상태 조회(M3)가 이 저장소를 읽기 전용으로 참조해야
 * 하므로, 그 마일스톤은 이 규칙에 {@code query} 패키지 예외를 추가해야 한다
 * (plan.md §F M3).</p>
 */
class EnrollmentQueueBoundaryArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.hongseob.openclass_ap");

    @Test
    void EnrollmentRequestRepository는_접수와_워커_패키지에서만_참조된다() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..enrollment.receipt..")
                .and().resideOutsideOfPackage("..enrollment.worker..")
                .and().resideOutsideOfPackage("..enrollment.request..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository");

        rule.check(CLASSES);
    }
}
