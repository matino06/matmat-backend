package com.example.EduSprint;

import com.example.EduSprint.storage.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class EduSprintApplication {

	public static void main(String[] args) {
		SpringApplication.run(EduSprintApplication.class, args);
	}

//	@Bean
//	CommandLineRunner runner(AccountObjectiveService service) {
//		return args -> {
//
//			System.out.println(service.getNextLearningObjective(Long.valueOf(15)));
//		};
//	}
}
