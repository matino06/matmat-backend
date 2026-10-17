package com.example.EduSprint.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Revokes a student's Sign in with Apple access when they delete their account, as Apple requires: the app sends
 * a fresh authorization code, which is exchanged for a refresh token and revoked. Requests are signed with the
 * "Sign in with Apple" key (APPLE_TEAM_ID, APPLE_KEY_ID, APPLE_PRIVATE_KEY = the .p8 file's contents) for the
 * app's bundle ID (APPLE_CLIENT_ID).
 */
@Component
public class AppleTokens {

    static final String APPLE = "https://appleid.apple.com";
    private static final Duration SECRET_LIFETIME = Duration.ofMinutes(5);

    private final RestClient http;
    private final String teamId;
    private final String keyId;
    private final String clientId;
    private final String privateKey;

    public AppleTokens(RestClient.Builder http,
                       @Value("${apple.team-id:}") String teamId,
                       @Value("${apple.key-id:}") String keyId,
                       @Value("${apple.client-id:}") String clientId,
                       @Value("${apple.private-key:}") String privateKey) {
        this.http = http.build();
        this.teamId = teamId;
        this.keyId = keyId;
        this.clientId = clientId;
        this.privateKey = privateKey;
    }

    public boolean configured() {
        return !teamId.isBlank() && !keyId.isBlank() && !clientId.isBlank() && !privateKey.isBlank();
    }

    /** Exchanges the authorization code and revokes the token it gives. */
    public void revoke(String authorizationCode) {
        String secret = clientSecret(Instant.now());
        MultiValueMap<String, String> exchange = form(secret);
        exchange.add("grant_type", "authorization_code");
        exchange.add("code", authorizationCode);
        Map<?, ?> tokens = post("/auth/token", exchange);

        boolean refresh = tokens != null && tokens.get("refresh_token") instanceof String;
        Object token = tokens == null ? null : tokens.get(refresh ? "refresh_token" : "access_token");
        if (!(token instanceof String value)) throw new IllegalStateException("Apple returned no token to revoke");
        MultiValueMap<String, String> revoke = form(secret);
        revoke.add("token", value);
        revoke.add("token_type_hint", refresh ? "refresh_token" : "access_token");
        post("/auth/revoke", revoke);
    }

    /** The client secret Apple's token endpoints take: a short-lived JWT signed with the key (ES256). */
    String clientSecret(Instant now) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(teamId)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(SECRET_LIFETIME)))
                    .audience(APPLE)
                    .subject(clientId)
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build(), claims);
            jwt.sign(new ECDSASigner(key(privateKey)));
            return jwt.serialize();
        } catch (GeneralSecurityException | JOSEException e) {
            throw new IllegalStateException("Sign in with Apple key can't sign: " + e.getMessage(), e);
        }
    }

    /** The .p8 contents, with real or escaped ("\n") line breaks, or just its base64 body. */
    static ECPrivateKey key(String pem) throws GeneralSecurityException {
        String body = pem.replace("\\n", "\n")
                .replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)));
    }

    private MultiValueMap<String, String> form(String secret) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", secret);
        return form;
    }

    private Map<?, ?> post(String path, MultiValueMap<String, String> form) {
        return http.post()
                .uri(APPLE + path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
    }
}
