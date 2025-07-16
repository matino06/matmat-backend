package com.example.EduSprint.controller;

import com.example.EduSprint.service.TurnstileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/turnstile")
public class TurnstileController {
    public final TurnstileService turnstileAuth;

    public TurnstileController(TurnstileService turnstileAuth) {
        this.turnstileAuth = turnstileAuth;
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyTrurnstile(@RequestBody String requestBody) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestBody);
        String token = jsonNode.get("token").asText();

        if (turnstileAuth.verifyTurnstileToken(token)) {
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.badRequest().body("Invalid token");
        }
    }

}
