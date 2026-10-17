package com.example.EduSprint.service;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class AppleTokensTest {

    private final KeyPair keys = keys();
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private static KeyPair keys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** The key as a .p8 file has it, with its line breaks escaped as Railway may store them. */
    private String p8() {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keys.getPrivate().getEncoded());
        return ("-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----").replace("\n", "\\n");
    }

    private AppleTokens tokens() {
        return new AppleTokens(builder, "TEAM123456", "KEY1234567", "online.matmat.app", p8());
    }

    @Test
    void clientSecretIsSignedWithTheKeyForTheApp() throws Exception {
        Instant now = Instant.parse("2026-10-17T10:00:00Z");
        SignedJWT jwt = SignedJWT.parse(tokens().clientSecret(now));

        assertTrue(jwt.verify(new ECDSAVerifier((ECPublicKey) keys.getPublic())));
        assertEquals("ES256", jwt.getHeader().getAlgorithm().getName());
        assertEquals("KEY1234567", jwt.getHeader().getKeyID());
        assertEquals("TEAM123456", jwt.getJWTClaimsSet().getIssuer());
        assertEquals("online.matmat.app", jwt.getJWTClaimsSet().getSubject());
        assertEquals(List.of("https://appleid.apple.com"), jwt.getJWTClaimsSet().getAudience());
        assertTrue(jwt.getJWTClaimsSet().getExpirationTime().toInstant().isAfter(now));
    }

    @Test
    void theCodeIsExchangedAndItsRefreshTokenRevoked() {
        server.expect(requestTo("https://appleid.apple.com/auth/token")).andExpect(method(POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code=apple-code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=authorization_code")))
                .andRespond(withSuccess("{\"access_token\":\"a\",\"refresh_token\":\"r-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://appleid.apple.com/auth/revoke")).andExpect(method(POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("token=r-token")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("token_type_hint=refresh_token")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_id=online.matmat.app")))
                .andRespond(withSuccess());

        tokens().revoke("apple-code");

        server.verify();
    }
}
