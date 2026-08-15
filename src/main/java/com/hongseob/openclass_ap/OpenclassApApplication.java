package com.hongseob.openclass_ap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpenclassApApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenclassApApplication.class, args);
	}

}
