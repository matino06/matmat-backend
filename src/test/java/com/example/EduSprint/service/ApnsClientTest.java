package com.example.EduSprint.service;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApnsClientTest {

    private static String p8() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    @Test
    void withoutTheKeyNothingIsSent() {
        ApnsClient apns = new ApnsClient("TEAM", "", "", "online.matmat.app");
        assertFalse(apns.configured());
        assertEquals(ApnsClient.Result.FAILED, apns.send("ab".repeat(32), false, "t", "b", Map.of()));
    }

    @Test
    void theProviderTokenIsSignedForTheTeamAndReusedForAWhile() throws Exception {
        ApnsClient apns = new ApnsClient("VMT2YS4585", "KEY123", p8(), "online.matmat.app");
        Instant now = Instant.parse("2026-10-30T10:00:00Z");
        String token = apns.providerToken(now);
        SignedJWT jwt = SignedJWT.parse(token);
        assertEquals("ES256", jwt.getHeader().getAlgorithm().getName());
        assertEquals("KEY123", jwt.getHeader().getKeyID());
        assertEquals("VMT2YS4585", jwt.getJWTClaimsSet().getIssuer());
        assertEquals(token, apns.providerToken(now.plusSeconds(30 * 60)));
        assertNotEquals(token, apns.providerToken(now.plusSeconds(50 * 60)));
    }

    @Test
    void thePayloadCarriesTheAlertAndWhereToOpen() throws Exception {
        String json = new ApnsClient("T", "K", "", "online.matmat.app")
                .payload("Naslov", "Tekst", Map.of("open", "exam", "attemptId", 7));
        assertTrue(json.contains("\"aps\":{"));
        assertTrue(json.contains("\"title\":\"Naslov\""));
        assertTrue(json.contains("\"open\":\"exam\""));
        assertTrue(json.contains("\"attemptId\":7"));
    }

    @Test
    void anUnknownOrDeletedDeviceIsForgotten() {
        assertEquals(ApnsClient.Result.SENT, ApnsClient.result(200, ""));
        assertEquals(ApnsClient.Result.GONE, ApnsClient.result(410, "{\"reason\":\"Unregistered\"}"));
        assertEquals(ApnsClient.Result.GONE, ApnsClient.result(400, "{\"reason\":\"BadDeviceToken\"}"));
        assertEquals(ApnsClient.Result.FAILED, ApnsClient.result(429, "{\"reason\":\"TooManyRequests\"}"));
    }
}
