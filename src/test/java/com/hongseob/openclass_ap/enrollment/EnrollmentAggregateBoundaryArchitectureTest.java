package com.hongseob.openclass_ap.enrollment;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-ENR-009 — 확정 경로 단일성의 우회 봉쇄를 매핑·구조 양면으로 검증한다
 * (design.md §5의 3층 검증 중 1·2층, M2). 3층(행동 검증)은
 * {@code EnrollmentWorkerDispatchIntegrationTest}의 AC-ENR-008이 담당한다.
 *
 * <p>(i) {@link Enrollment}를 대상으로 하는 {@code CascadeType.PERSIST}/{@code ALL}
 * 매핑이 존재하는지 전체 {@code @Entity} 클래스를 리플렉션으로 순회하며
 * 확인한다 — JPA {@code Metamodel} API는 캐스케이드 정보를 노출하지 않으므로
 * (Hibernate 내부 SPI 영역), 애노테이션 자체를 직접 검사하는 편이 더 안정적이고
 * 이 검증의 의도("`Course`에서 `Enrollment`로 연쇄 저장 설정을 두지 않는다",
 * plan.md §C.4)에 정확히 대응한다.</p>
 *
 * <p>(ii) {@link EnrollmentQueueBoundaryArchitectureTest}가 큐 저장소
 * ({@code EnrollmentRequestRepository})를 접수/워커/큐 패키지로 한정한 것과
 * 대칭으로, 이 테스트는 확정 저장소({@link EnrollmentRepository})와 확정
 * 엔티티({@link Enrollment}) 자체를 워커 패키지로 한정한다 — 컨트롤러·접수
 * 서비스가 이 둘을 직접 참조하는 경로를 구조적으로 차단한다.</p>
 *
 * <p><b>M4 예외 추가 근거</b>: {@code receipt.EnrollmentReceiptService}가
 * 취소 접수의 1차 소유권 검증(REQ-CNL-002, AC-ENR-036)을 위해 {@code
 * EnrollmentRepository}의 프로젝션 조회 메서드({@code
 * findCourseIdByIdAndMemberIdAndStatus} — {@code Enrollment} 엔티티가 아닌
 * courseId 원시 값만 반환한다)를 참조한다. 이 규칙이 막는 것은 "확정 생성·
 * 변경 경로가 워커 1개소를 벗어나는 것"(INV-ENR-002)이며, 존재·소유권 확인을
 * 위한 읽기 전용 조회는 그 위협 모델에 해당하지 않는다 — M3가 상태 조회
 * 패키지에 세운 것과 동일한 선례다({@link
 * EnrollmentQueueBoundaryArchitectureTest} "M3 예외 추가 근거" 참고). {@code
 * Enrollment} 엔티티 자체는 여전히 워커 패키지 밖에서 참조할 수 없으므로
 * 아래 3번째 테스트는 그대로 유지한다.</p>
 *
 * <p><b>4번째 테스트 추가 근거</b> (SPEC-ENROLLMENT-001 sync-audit F3): {@code
 * course.CourseEnrolledCountMutationAbsenceTest}가 {@code
 * CourseCapacityRepository.java}를 제외 목록에 추가하며 "ArchUnit이 이 경계를
 * 별도로 강제한다"고 인용했으나, 그 시점에는 실제로 그런 규칙이 존재하지
 * 않았다(미검증 주장). 아래 4번째 테스트가 그 인용을 사실로 만든다 — {@code
 * enrolled_count}를 변경하는 유일한 합법 경로(REQ-WRK-002)가 워커 패키지 밖의
 * 임의 Spring 빈 주입으로 우회되지 않음을 구조적으로 보장한다.</p>
 */
class EnrollmentAggregateBoundaryArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.hongseob.openclass_ap");

    // AC-ENR-009 (i) — 매핑 제약
    @Test
    void Enrollment을_대상으로_하는_CascadeType_PERSIST_또는_ALL_매핑이_0건이다() {
        List<String> violations = new ArrayList<>();
        for (JavaClass javaClass : CLASSES) {
            if (!javaClass.isAnnotatedWith(Entity.class)) {
                continue;
            }
            Class<?> entityClass = javaClass.reflect();
            for (Field field : entityClass.getDeclaredFields()) {
                if (!referencesEnrollment(field)) {
                    continue;
                }
                for (CascadeType cascadeType : cascadeOf(field)) {
                    if (cascadeType == CascadeType.PERSIST || cascadeType == CascadeType.ALL) {
                        violations.add(entityClass.getSimpleName() + "." + field.getName() + " -> " + cascadeType);
                    }
                }
            }
        }
        assertThat(violations)
                .as("Enrollment를 대상으로 하는 CascadeType.PERSIST/ALL 매핑")
                .isEmpty();
    }

    // AC-ENR-009 (ii) — 구조적 검증 (EnrollmentRepository).
    // M4 예외: receipt 패키지는 취소 접수의 1차 소유권 검증(프로젝션
    // 조회, Enrollment 엔티티는 노출하지 않음)을 위해 참조가 허용된다
    // (클래스 Javadoc "M4 예외 추가 근거" 참고).
    @Test
    void EnrollmentRepository는_워커와_접수_패키지에서만_참조된다() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..enrollment.worker..")
                .and().resideOutsideOfPackage("..enrollment.receipt..")
                .and().doNotHaveFullyQualifiedName("com.hongseob.openclass_ap.enrollment.EnrollmentRepository")
                .and().doNotHaveFullyQualifiedName("com.hongseob.openclass_ap.enrollment.Enrollment")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.hongseob.openclass_ap.enrollment.EnrollmentRepository");

        rule.check(CLASSES);
    }

    // AC-ENR-009 (ii) — 구조적 검증 (Enrollment 애그리게이트)
    @Test
    void Enrollment_애그리게이트는_워커_패키지에서만_참조된다() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..enrollment.worker..")
                .and().doNotHaveFullyQualifiedName("com.hongseob.openclass_ap.enrollment.EnrollmentRepository")
                .and().doNotHaveFullyQualifiedName("com.hongseob.openclass_ap.enrollment.Enrollment")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.hongseob.openclass_ap.enrollment.Enrollment");

        rule.check(CLASSES);
    }

    // sync-audit F3 — CourseCapacityRepository는 enrolled_count를 변경하는
    // 유일한 합법 경로(REQ-WRK-002)이므로 워커 패키지 밖에서 참조되면 안 된다.
    @Test
    void CourseCapacityRepository는_워커_패키지에서만_참조된다() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..enrollment.worker..")
                .and().doNotHaveFullyQualifiedName(
                        "com.hongseob.openclass_ap.enrollment.worker.CourseCapacityRepository")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.hongseob.openclass_ap.enrollment.worker.CourseCapacityRepository");

        rule.check(CLASSES);
    }

    private static boolean referencesEnrollment(Field field) {
        if (field.getType().equals(Enrollment.class)) {
            return true;
        }
        if (Collection.class.isAssignableFrom(field.getType())
                && field.getGenericType() instanceof ParameterizedType parameterizedType) {
            return Arrays.stream(parameterizedType.getActualTypeArguments())
                    .map(Type::getTypeName)
                    .anyMatch(name -> name.equals(Enrollment.class.getName()));
        }
        return false;
    }

    private static CascadeType[] cascadeOf(Field field) {
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            return oneToMany.cascade();
        }
        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (oneToOne != null) {
            return oneToOne.cascade();
        }
        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        if (manyToMany != null) {
            return manyToMany.cascade();
        }
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        if (manyToOne != null) {
            return manyToOne.cascade();
        }
        return new CascadeType[0];
    }
}
