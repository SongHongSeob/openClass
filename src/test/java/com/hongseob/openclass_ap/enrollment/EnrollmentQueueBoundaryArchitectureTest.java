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
 * 엔티티 자신(request)·상태 조회(query) 패키지로 한정한다 — 다른 패키지가 이
 * 저장소에 직접 접근해 잠금을 우회한 채 큐 행을 만드는 경로를 구조적으로
 * 차단한다.
 *
 * <p><b>M3 예외 추가 근거</b>: {@link
 * com.hongseob.openclass_ap.enrollment.query.EnrollmentStatusQueryService}가
 * 상태 조회를 위해 이 저장소를 **읽기 전용**({@code @Transactional(readOnly =
 * true)})으로 참조한다(REQ-STS-001, REQ-STS-004). 이 규칙이 막는 것은 "잠금
 * 없는 큐 행 INSERT 경로"이며, 읽기 전용 조회는 그 위협 모델에 해당하지
 * 않는다 — M1 Javadoc이 이미 이 예외를 예정해 두었다(plan.md §F M3).</p>
 */
class EnrollmentQueueBoundaryArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.hongseob.openclass_ap");

    @Test
    void EnrollmentRequestRepository는_접수와_워커와_상태조회_패키지에서만_참조된다() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..enrollment.receipt..")
                .and().resideOutsideOfPackage("..enrollment.worker..")
                .and().resideOutsideOfPackage("..enrollment.request..")
                .and().resideOutsideOfPackage("..enrollment.query..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository");

        rule.check(CLASSES);
    }
}
