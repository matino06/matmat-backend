package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FcmClientTest {

    private static String serviceAccount() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        return new ObjectMapper().writeValueAsString(Map.of(
                "type", "service_account", "project_id", "matmat-online",
                "client_email", "firebase-adminsdk@matmat-online.iam.gserviceaccount.com",
                "private_key", pem, "token_uri", "https://oauth2.googleapis.com/token"));
    }

    @Test
    void withoutTheKeyNothingIsSent() {
        FcmClient fcm = new FcmClient("");
        assertFalse(fcm.configured());
        assertEquals(ApnsClient.Result.FAILED, fcm.send("token", "t", "b", Map.of()));
    }

    @Test
    void theAssertionIsSignedByTheServiceAccountForMessaging() throws Exception {
        FcmClient fcm = new FcmClient(serviceAccount());
        assertTrue(fcm.configured());
        SignedJWT jwt = SignedJWT.parse(fcm.assertion(Instant.parse("2026-11-04T10:00:00Z")));
        assertEquals("RS256", jwt.getHeader().getAlgorithm().getName());
        assertEquals("firebase-adminsdk@matmat-online.iam.gserviceaccount.com", jwt.getJWTClaimsSet().getIssuer());
        assertEquals("https://oauth2.googleapis.com/token", jwt.getJWTClaimsSet().getAudience().get(0));
        assertEquals("https://www.googleapis.com/auth/firebase.messaging", jwt.getJWTClaimsSet().getStringClaim("scope"));
    }

    @Test
    void thePayloadIsANotificationOnTheAppsChannelWithStringData() throws Exception {
        JsonNode message = new ObjectMapper().readTree(new FcmClient("")
                .payload("tok", "Naslov", "Tekst", Map.of("open", "exam", "attemptId", 7))).path("message");
        assertEquals("tok", message.path("token").asText());
        assertEquals("Naslov", message.path("notification").path("title").asText());
        assertEquals("exam", message.path("data").path("open").asText());
        assertTrue(message.path("data").path("attemptId").isTextual());
        assertEquals(FcmClient.CHANNEL, message.path("android").path("notification").path("channel_id").asText());
    }

    @Test
    void anUnregisteredTokenIsForgotten() {
        assertEquals(ApnsClient.Result.SENT, FcmClient.result(200, "{}"));
        assertEquals(ApnsClient.Result.GONE, FcmClient.result(404, "{\"error\":{\"status\":\"NOT_FOUND\"}}"));
        assertEquals(ApnsClient.Result.GONE, FcmClient.result(400, "The registration token is not a valid FCM registration token"));
        assertEquals(ApnsClient.Result.FAILED, FcmClient.result(500, "oops"));
    }
}
