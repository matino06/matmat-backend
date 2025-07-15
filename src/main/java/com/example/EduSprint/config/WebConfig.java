package com.example.EduSprint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")  // Dozvoljava CORS za sve endpointove
                        .allowedOrigins("http://localhost:5173", "https://www.matmat.online/", "https://matmat.online/", "https://matmat1.netlify.app/")  // Dozvoljeni origin-i
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // Dozvoljeni HTTP metodi
                        .allowedHeaders("*")  // Dozvoljeni header-i
                        .allowCredentials(true);  // Dozvoljava kolačiće (credentials)
            }
        };
    }
}