package com.example.EduSprint.controller;

import com.example.EduSprint.service.AccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    @RestController
    static class Throwing {
        @GetMapping("/missing-account")
        String missingAccount() {
            throw new AccountNotFoundException();
        }

        @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        String stream() {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Dnevni limit je potrošen");
        }

        @GetMapping("/limit")
        String limit() {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Dnevni limit je potrošen");
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Throwing())
            .setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void missingAccountIs404WithMessage() throws Exception {
        mvc.perform(get("/missing-account"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Račun ne postoji"));
    }

    @Test
    void streamingEndpointsStillAnswerErrorsWithTheirStatus() throws Exception {
        // AI chat is text/event-stream; a 429 before the stream starts must stay 429, not become 406.
        mvc.perform(get("/stream"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Dnevni limit je potrošen"));
        mvc.perform(get("/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void statusExceptionsKeepTheirStatusAndReason() throws Exception {
        mvc.perform(get("/limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Dnevni limit je potrošen"));
    }
}
