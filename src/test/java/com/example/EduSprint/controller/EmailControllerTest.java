package com.example.EduSprint.controller;

import com.example.EduSprint.service.EmailUnsubscribeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmailControllerTest {

    private final EmailUnsubscribeService service = mock(EmailUnsubscribeService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new EmailController(service)).build();

    @Test
    void openingTheLinkOnlyShowsTheButton() throws Exception {
        when(service.isValid(42L, "tok")).thenReturn(true);

        mvc.perform(get("/email/unsubscribe/42/tok"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<form method=\"post\">")));

        verify(service, never()).unsubscribe(anyLong(), any());
    }

    @Test
    void oneClickPostUnsubscribes() throws Exception {
        when(service.unsubscribe(42L, "tok")).thenReturn(true);

        mvc.perform(post("/email/unsubscribe/42/tok")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("List-Unsubscribe=One-Click"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Odjavljen/a si")));
    }

    @Test
    void invalidLinkIsNotFound() throws Exception {
        mvc.perform(get("/email/unsubscribe/42/forged"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/email/unsubscribe/42/forged"))
                .andExpect(status().isNotFound());
    }
}
