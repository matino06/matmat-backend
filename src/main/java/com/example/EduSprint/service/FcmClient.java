package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends alerts to Android devices through Firebase Cloud Messaging (HTTP v1). Signed in as the Firebase project's
 * service account (FCM_SERVICE_ACCOUNT = the key's JSON file, as Google gives it). Without it nothing is sent.
 */
@Component
public class FcmClient {

    private static final Logger log = LoggerFactory.getLogger(FcmClient.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    /** The app's channel for these alerts (online.matmat.app.focus.Channels.PUSH). */
    static final String CHANNEL = "matmat-push";
    // Google's access tokens last an hour; a new one a little before that.
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(50);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final String projectId;
    private final String clientEmail;
    private final String privateKey;
    private final String tokenUri;
    private String accessToken;
    private Instant accessIssued = Instant.EPOCH;

    public FcmClient(@Value("${fcm.service-account:}") String serviceAccount) {
        JsonNode account = parse(serviceAccount);
        this.projectId = text(account, "project_id");
        this.clientEmail = text(account, "client_email");
        this.privateKey = text(account, "private_key");
        String uri = text(account, "token_uri");
        this.tokenUri = uri.isBlank() ? "https://oauth2.googleapis.com/token" : uri;
    }

    public boolean configured() {
        return !projectId.isBlank() && !clientEmail.isBlank() && !privateKey.isBlank();
    }

    /**
     * @param data extra keys for the app to open the right screen (FCM takes only string values)
     * @return GONE when the registration token is no longer valid and should be forgotten
     */
    public ApnsClient.Result send(String deviceToken, String title, String body, Map<String, Object> data) {
        if (!configured()) {
            log.warn("FCM: not sent, FCM_SERVICE_ACCOUNT is missing");
            return ApnsClient.Result.FAILED;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send"))
                    .timeout(Duration.ofSeconds(15))
                    .header("authorization", "Bearer " + accessToken(Instant.now()))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload(deviceToken, title, body, data)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return result(response.statusCode(), response.body());
        } catch (IOException | RuntimeException e) {
            log.warn("FCM: sending failed: {}", e.getMessage());
            return ApnsClient.Result.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ApnsClient.Result.FAILED;
        }
    }

    /** A notification the system shows on the app's channel, with the data for opening the right tab. */
    String payload(String deviceToken, String title, String body, Map<String, Object> data) throws IOException {
        Map<String, String> strings = new LinkedHashMap<>();
        data.forEach((k, v) -> strings.put(k, String.valueOf(v)));
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", deviceToken);
        message.put("notification", Map.of("title", title, "body", body));
        message.put("data", strings);
        message.put("android", Map.of("priority", "high", "notification", Map.of("channel_id", CHANNEL)));
        return json.writeValueAsString(Map.of("message", message));
    }

    /** 404 UNREGISTERED: the app was removed or the token replaced; a 400 about the token: it was never valid. */
    static ApnsClient.Result result(int status, String body) {
        if (status == 200) return ApnsClient.Result.SENT;
        if (status == 404 || (body != null && body.contains("UNREGISTERED"))) return ApnsClient.Result.GONE;
        if (status == 400 && body != null && body.contains("registration token")) return ApnsClient.Result.GONE;
        log.warn("FCM: {} {}", status, body);
        return ApnsClient.Result.FAILED;
    }

    /** The OAuth access token for the service account (a signed JWT exchanged at Google's token endpoint). */
    synchronized String accessToken(Instant now) throws IOException, InterruptedException {
        if (accessToken != null && now.isBefore(accessIssued.plus(TOKEN_LIFETIME))) return accessToken;
        String form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8)
                + "&assertion=" + URLEncoder.encode(assertion(now), StandardCharsets.UTF_8);
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(tokenUri))
                .timeout(Duration.ofSeconds(15))
                .header("content-type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("token " + response.statusCode() + " " + response.body());
        accessToken = json.readTree(response.body()).path("access_token").asText();
        accessIssued = now;
        return accessToken;
    }

    String assertion(Instant now) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(clientEmail)
                    .audience(tokenUri)
                    .claim("scope", SCOPE)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), claims);
            jwt.sign(new RSASSASigner(key(privateKey)));
            return jwt.serialize();
        } catch (GeneralSecurityException | JOSEException e) {
            throw new IllegalStateException("FCM key can't sign: " + e.getMessage(), e);
        }
    }

    private static RSAPrivateKey key(String pem) throws GeneralSecurityException {
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    private JsonNode parse(String serviceAccount) {
        if (serviceAccount == null || serviceAccount.isBlank()) return json.createObjectNode();
        try {
            return json.readTree(serviceAccount);
        } catch (IOException e) {
            log.warn("FCM: FCM_SERVICE_ACCOUNT is not the key's JSON: {}", e.getMessage());
            return json.createObjectNode();
        }
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }
}
