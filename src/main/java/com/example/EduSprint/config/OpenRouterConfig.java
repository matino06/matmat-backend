package com.example.EduSprint.config;

import com.example.EduSprint.service.OpenRouterClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenRouterConfig {

    // Odvojeni ključevi da se potrošnja ocjenjivanja i chata vidi (i ograničava) zasebno na OpenRouteru.
    @Bean(name = "gradingOpenRouterClient")
    public OpenRouterClient gradingOpenRouterClient(@Value("${openrouter.api-key}") String apiKey,
                                                    @Value("${openrouter.api-base-url}") String baseUrl) {
        return new OpenRouterClient(apiKey, baseUrl);
    }

    @Bean(name = "chatOpenRouterClient")
    public OpenRouterClient chatOpenRouterClient(@Value("${ai.chat.api-key}") String apiKey,
                                                 @Value("${openrouter.api-base-url}") String baseUrl) {
        return new OpenRouterClient(apiKey, baseUrl);
    }
}
