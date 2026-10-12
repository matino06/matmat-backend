package com.example.EduSprint.service;

import com.example.EduSprint.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

// Unsubscribe links for reminder emails. The link carries the account id signed with a server
// secret (HMAC), so it works without logging in and cannot be forged for another account.
@Service
public class EmailUnsubscribeService {

    private final AccountRepository accountRepository;
    private final String publicApiUrl;
    private final byte[] secret;

    public EmailUnsubscribeService(AccountRepository accountRepository,
                                   @Value("${app.public-api-url}") String publicApiUrl,
                                   @Value("${email.unsubscribe-secret}") String secret) {
        this.accountRepository = accountRepository;
        this.publicApiUrl = publicApiUrl;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String unsubscribeUrl(Long accountId) {
        return publicApiUrl + "/email/unsubscribe/" + accountId + "/" + token(accountId);
    }

    public boolean isValid(Long accountId, String token) {
        return MessageDigest.isEqual(
                token(accountId).getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    // Turns reminder emails off. Returns false for an invalid link or an unknown account.
    public boolean unsubscribe(Long accountId, String token) {
        if (!isValid(accountId, token)) {
            return false;
        }
        return accountRepository.findById(accountId)
                .map(account -> {
                    account.setLearningRemindersEnabled(false);
                    accountRepository.save(account);
                    return true;
                })
                .orElse(false);
    }

    private String token(Long accountId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] signature = mac.doFinal(("unsubscribe:" + accountId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
