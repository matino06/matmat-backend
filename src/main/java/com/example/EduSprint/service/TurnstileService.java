package com.example.EduSprint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TurnstileService {
    @Value("${turnstile.verify-url}")
    private String CLOUDFLARE_VERIFY_URL;

    @Value("${turnstile.secret-key}")
    private String CLOUDFLARE_SECRET_KEY;

    public boolean verifyTurnstileToken(String cfToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", CLOUDFLARE_SECRET_KEY);
        body.add("response", cfToken);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(CLOUDFLARE_VERIFY_URL, requestEntity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        return responseBody != null && Boolean.TRUE.equals(responseBody.get("success"));
    }
}
