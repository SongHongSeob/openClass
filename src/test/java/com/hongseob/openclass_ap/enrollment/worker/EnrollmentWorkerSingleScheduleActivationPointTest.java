package com.hongseob.openclass_ap.enrollment.worker;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-ENR-019 — 워커 스케줄러를 활성화하는 지점이 정확히 1개소임을
 * 구조적으로 검증한다(REQ-WRK-012). 단일 워커 인스턴스 전제와 다중
 * 인스턴스에서 순서 보장이 성립하지 않는다는 경고는 {@code README.md}
 * "수강신청 큐·워커" 절에 문서화되어 있다 — 이 테스트는 그중 "스케줄러
 * 활성화 지점 1개소" 부분을 기계적으로 확인한다(M2).
 */
class EnrollmentWorkerSingleScheduleActivationPointTest {

    @Test
    void Scheduled_애노테이션이_붙은_메서드는_전체_프로덕션_코드에서_EnrollmentQueueWorker_poll_1개소뿐이다() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.hongseob.openclass_ap");

        int scheduledMethodCount = 0;
        JavaMethod pollMethod = null;
        for (JavaClass javaClass : classes) {
            for (JavaMethod method : javaClass.getMethods()) {
                if (method.isAnnotatedWith(Scheduled.class)) {
                    scheduledMethodCount++;
                    if (javaClass.reflect().equals(EnrollmentQueueWorker.class)) {
                        pollMethod = method;
                    }
                }
            }
        }

        assertThat(scheduledMethodCount).as("@Scheduled 메서드는 정확히 1개소여야 한다").isEqualTo(1);
        assertThat(pollMethod).as("유일한 @Scheduled 메서드는 EnrollmentQueueWorker에 있어야 한다").isNotNull();
        assertThat(pollMethod.getName()).isEqualTo("poll");
    }
}
