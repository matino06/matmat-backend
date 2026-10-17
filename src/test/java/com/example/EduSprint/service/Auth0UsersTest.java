package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Auth0UsersTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @Test
    void eachLoginIsDeletedWithAManagementToken() {
        server.expect(requestTo("https://matmat.eu.auth0.com/oauth/token")).andExpect(method(POST))
                .andExpect(content().string(containsString("\"audience\":\"https://matmat.eu.auth0.com/api/v2/\"")))
                .andRespond(withSuccess("{\"access_token\":\"mgmt\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://matmat.eu.auth0.com/api/v2/users/google-oauth2%7C1")).andExpect(method(DELETE))
                .andExpect(header("Authorization", "Bearer mgmt"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        server.expect(requestTo("https://matmat.eu.auth0.com/api/v2/users/apple%7C001.abc")).andExpect(method(DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        new Auth0Users(builder, "matmat.eu.auth0.com", "id", "secret").delete(List.of("google-oauth2|1", "apple|001.abc"));

        server.verify();
    }

    @Test
    void withoutCredentialsItIsNotConfigured() {
        assertFalse(new Auth0Users(builder, "matmat.eu.auth0.com", "", "").configured());
    }
}
