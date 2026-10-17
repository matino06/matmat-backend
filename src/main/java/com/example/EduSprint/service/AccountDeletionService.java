package com.example.EduSprint.service;

import com.example.EduSprint.security.AccountLogins;
import com.example.EduSprint.security.AccountResolver;
import com.example.EduSprint.storage.MaterialStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Obriši račun" from the app or the web: everything of the account is deleted at once and for good.
 *
 * <p>Order: Apple access is revoked first (the app's code is valid for 5 minutes), then every row goes in one
 * transaction. Only after that commit are the S3 files, the Auth0 users and the confirmation email handled; those
 * failures are logged and don't fail the request, because the account itself is already gone.
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final AccountLogins logins;
    private final AccountResolver resolver;
    private final AccountDataDeleter rows;
    private final MaterialStorage storage;
    private final Auth0Users auth0;
    private final AppleTokens apple;
    private final EmailService email;

    public AccountDeletionService(AccountLogins logins, AccountResolver resolver, AccountDataDeleter rows,
                                  MaterialStorage storage, Auth0Users auth0, AppleTokens apple, EmailService email) {
        this.logins = logins;
        this.resolver = resolver;
        this.rows = rows;
        this.storage = storage;
        this.auth0 = auth0;
        this.apple = apple;
        this.email = email;
    }

    /**
     * @param accountEmail      the account's email (the resolved one, see {@link AccountResolver})
     * @param loginSubject      the Auth0 user the request came from; deleted with the account's other logins
     * @param appleAuthorization a fresh Sign in with Apple code from the app, or null
     */
    public void delete(String accountEmail, String loginSubject, String appleAuthorization) {
        long accountId = logins.accountIdForEmail(accountEmail).orElseThrow(AccountNotFoundException::new);
        Set<String> users = new LinkedHashSet<>(logins.subjectsOf(accountId));
        if (loginSubject != null && !loginSubject.isBlank()) users.add(loginSubject);

        if (appleAuthorization != null && !appleAuthorization.isBlank()) {
            if (!apple.configured()) {
                log.warn("Account {}: Apple access not revoked, APPLE_* variables are missing", accountId);
            } else {
                try {
                    apple.revoke(appleAuthorization);
                } catch (RuntimeException e) {
                    log.warn("Account {}: revoking Apple access failed: {}", accountId, e.getMessage());
                }
            }
        }

        List<Long> attempts = rows.deleteRows(accountId);
        resolver.forgetEmail(accountEmail);
        log.info("Account {} deleted ({} logins, {} mock exam attempts)", accountId, users.size(), attempts.size());

        run(accountId, "deleting own course files", () -> storage.deleteAccountFolder(accountId));
        for (Long attempt : attempts) {
            run(accountId, "deleting answer photos of attempt " + attempt, () -> storage.deleteAnswerImages(attempt));
        }
        if (auth0.configured()) {
            run(accountId, "deleting Auth0 users", () -> auth0.delete(users));
        } else {
            log.warn("Account {}: Auth0 users {} not deleted, AUTH0_MGMT_* variables are missing", accountId, users);
        }
        run(accountId, "sending the confirmation email", () -> email.sendAccountDeletedEmail(accountEmail));
    }

    private static void run(long accountId, String what, Runnable step) {
        try {
            step.run();
        } catch (RuntimeException e) {
            log.warn("Account {}: {} failed after the account was deleted: {}", accountId, what, e.getMessage());
        }
    }
}
