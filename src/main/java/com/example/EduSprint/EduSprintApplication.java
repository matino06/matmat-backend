package com.example.EduSprint;

import com.example.EduSprint.service.LearningObjectiveService;
import com.example.EduSprint.storage.StorageProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class EduSprintApplication {

	public static void main(String[] args) {
		SpringApplication.run(EduSprintApplication.class, args);
	}
}
