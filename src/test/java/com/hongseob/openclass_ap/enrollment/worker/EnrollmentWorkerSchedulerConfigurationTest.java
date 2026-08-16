package com.hongseob.openclass_ap.enrollment.worker;

import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC-ENR-022 — 워커의 실제 폴링 주기·배치 크기 설정값이 design.md §6
 * 산출표(폴링 200ms · 배치 200건 · 이론 처리량 1,000건/초)와 일치한다(M2).
 * 어긋나는 경우 실측치에 근거한 개정 기록을 progress.md §E.2에 남겨야 하지만,
 * 이 마일스톤은 산출표 값을 그대로 채택했으므로 개정 기록이 없다.
 */
@SpringBootTest
class EnrollmentWorkerSchedulerConfigurationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentSchedulerProperties properties;

    @Test
    void 워커_설정값이_design_md_6_산출표와_일치한다() {
        assertThat(properties.pollingDelayMs()).isEqualTo(200L);
        assertThat(properties.batchSize()).isEqualTo(200);
    }
}
