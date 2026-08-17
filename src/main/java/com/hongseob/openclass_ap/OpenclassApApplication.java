package com.hongseob.openclass_ap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// SPEC-ENROLLMENT-001 REQ-WRK-012/AC-ENR-019 — @EnableScheduling은 이
// 프로젝트에서 이 지점 1개소에서만 선언한다. 워커의 유일한 @Scheduled 지점은
// EnrollmentQueueWorker.poll()이다 — README.md "수강신청 워커" 절 참고.
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class OpenclassApApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenclassApApplication.class, args);
	}

}
