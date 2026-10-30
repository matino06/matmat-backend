package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Sends alerts to iOS devices through Apple Push Notification service (HTTP/2, token auth). Signed with the APNs key
 * (APNS_KEY_ID, APNS_PRIVATE_KEY = the .p8 file's contents) of the team APNS_TEAM_ID, for the app APNS_TOPIC.
 * Without the key nothing is sent.
 */
@Component
public class ApnsClient {

    private static final Logger log = LoggerFactory.getLogger(ApnsClient.class);
    static final String PRODUCTION = "https://api.push.apple.com";
    static final String SANDBOX = "https://api.sandbox.push.apple.com";
    // Apple rejects a token older than an hour and one renewed more often than every 20 minutes.
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(45);

    public enum Result { SENT, GONE, FAILED }

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final String teamId;
    private final String keyId;
    private final String privateKey;
    private final String topic;
    private String token;
    private Instant tokenIssued = Instant.EPOCH;

    public ApnsClient(@Value("${apns.team-id:}") String teamId,
                      @Value("${apns.key-id:}") String keyId,
                      @Value("${apns.private-key:}") String privateKey,
                      @Value("${apns.topic:online.matmat.app}") String topic) {
        this.teamId = teamId;
        this.keyId = keyId;
        this.privateKey = privateKey;
        this.topic = topic;
    }

    public boolean configured() {
        return !teamId.isBlank() && !keyId.isBlank() && !privateKey.isBlank();
    }

    /**
     * @param data extra keys next to "aps", for the app to open the right screen
     * @return GONE when the device token is no longer valid and should be forgotten
     */
    public Result send(String deviceToken, boolean sandbox, String title, String body, Map<String, Object> data) {
        if (!configured()) {
            log.warn("APNs: not sent, APNS_* variables are missing");
            return Result.FAILED;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create((sandbox ? SANDBOX : PRODUCTION) + "/3/device/" + deviceToken))
                    .timeout(Duration.ofSeconds(15))
                    .header("authorization", "bearer " + providerToken(Instant.now()))
                    .header("apns-topic", topic)
                    .header("apns-push-type", "alert")
                    .header("apns-priority", "10")
                    .POST(HttpRequest.BodyPublishers.ofString(payload(title, body, data)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return result(response.statusCode(), response.body());
        } catch (IOException | RuntimeException e) {
            log.warn("APNs: sending failed: {}", e.getMessage());
            return Result.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.FAILED;
        }
    }

    String payload(String title, String body, Map<String, Object> data) throws IOException {
        Map<String, Object> aps = Map.of("alert", Map.of("title", title, "body", body), "sound", "default");
        java.util.LinkedHashMap<String, Object> all = new java.util.LinkedHashMap<>(data);
        all.put("aps", aps);
        return json.writeValueAsString(all);
    }

    /** 410, or a token APNs doesn't know for this app: the app was deleted or the token is from another environment. */
    static Result result(int status, String body) {
        if (status == 200) return Result.SENT;
        if (status == 410) return Result.GONE;
        if (status == 400 && body != null && (body.contains("BadDeviceToken") || body.contains("DeviceTokenNotForTopic"))) {
            return Result.GONE;
        }
        log.warn("APNs: {} {}", status, body);
        return Result.FAILED;
    }

    synchronized String providerToken(Instant now) {
        if (token != null && now.isBefore(tokenIssued.plus(TOKEN_LIFETIME))) return token;
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(teamId).issueTime(Date.from(now)).build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build(), claims);
            jwt.sign(new ECDSASigner(AppleTokens.key(privateKey)));
            token = jwt.serialize();
            tokenIssued = now;
            return token;
        } catch (GeneralSecurityException | JOSEException e) {
            throw new IllegalStateException("APNs key can't sign: " + e.getMessage(), e);
        }
    }
}
