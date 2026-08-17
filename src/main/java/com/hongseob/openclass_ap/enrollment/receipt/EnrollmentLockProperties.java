package com.hongseob.openclass_ap.enrollment.receipt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 접수 잠금 활성화 스위치 (plan.md §E 사전 점검 4번 / research.md §6 원칙 4).
 *
 * <p>기본값은 {@code true}다. AC-ENR-007(잠금 제거 대조군)만 테스트 프로퍼티로
 * {@code app.enrollment.lock-enabled=false}를 오버라이드하여, 잠금을 끄면 접수
 * 순서 보장이 실제로 깨진다는 것을 증명한다 — 대조군이 없으면 AC-ENR-006의
 * 통과가 우연이 아니라는 것을 증명할 수 없다(research.md §6-4).</p>
 */
@ConfigurationProperties(prefix = "app.enrollment")
public record EnrollmentLockProperties(@DefaultValue("true") boolean lockEnabled) {
}
