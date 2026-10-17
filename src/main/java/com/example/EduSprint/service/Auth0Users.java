package com.example.EduSprint.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.Map;

/**
 * Deletes Auth0 users through the Management API, with a machine-to-machine app that may only {@code delete:users}
 * (AUTH0_MGMT_CLIENT_ID and AUTH0_MGMT_CLIENT_SECRET). The Management API lives on the tenant's own domain, not on
 * the custom login domain.
 */
@Component
public class Auth0Users {

    private final RestClient http;
    private final String domain;
    private final String clientId;
    private final String clientSecret;

    public Auth0Users(RestClient.Builder http,
                      @Value("${auth0.management.domain}") String domain,
                      @Value("${auth0.management.client-id:}") String clientId,
                      @Value("${auth0.management.client-secret:}") String clientSecret) {
        this.http = http.build();
        this.domain = domain;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean configured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    /** Deletes the users (their Auth0 ids, e.g. {@code apple|001234.abcd}). Auth0 answers 204 for an unknown id too. */
    public void delete(Collection<String> userIds) {
        if (userIds.isEmpty()) return;
        String token = token();
        for (String userId : userIds) {
            http.delete()
                    .uri("https://{domain}/api/v2/users/{id}", domain, userId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    private String token() {
        Map<?, ?> body = http.post()
                .uri("https://{domain}/oauth/token", domain)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("grant_type", "client_credentials", "client_id", clientId, "client_secret", clientSecret,
                        "audience", "https://" + domain + "/api/v2/"))
                .retrieve()
                .body(Map.class);
        Object token = body == null ? null : body.get("access_token");
        if (!(token instanceof String s) || s.isBlank()) throw new IllegalStateException("Auth0 returned no access token");
        return s;
    }
}
