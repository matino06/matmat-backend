package com.example.EduSprint.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Turns a validated Auth0 access token into the {@link AuthPrincipal} the
 * controllers expect. Email is mandatory (every account is keyed by email); the
 * token is rejected with 401 if it carries no email claim.
 *
 * <p>Auth0 access tokens for a custom API don't include email/name by default —
 * a post-login Action must add them, ideally as namespaced claims. This reads the
 * namespaced claim first, then falls back to the standard {@code email}/{@code name}.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final String namespace;

    public JwtAuthConverter(@Value("${auth0.claims-namespace}") String namespace) {
        this.namespace = namespace;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = firstNonBlank(jwt.getClaimAsString(namespace + "email"),
                jwt.getClaimAsString("email"));
        if (email == null) {
            throw new InvalidBearerTokenException("Access token is missing the required email claim");
        }

        String name = firstNonBlank(jwt.getClaimAsString(namespace + "name"),
                jwt.getClaimAsString("name"));
        if (name == null) {
            name = email.substring(0, email.indexOf('@'));
        }

        AuthPrincipal principal = new AuthPrincipal(jwt.getSubject(), email, name);
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
