package com.example.EduSprint.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppVersionControllerTest {

    private MockMvc mvc(String minimum, String latest) {
        return MockMvcBuilders.standaloneSetup(new AppVersionController(minimum, latest)).build();
    }

    @Test
    void returnsTheConfiguredVersions() throws Exception {
        mvc("1.2", "1.3").perform(get("/app-version/ios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimum").value("1.2"))
                .andExpect(jsonPath("$.latest").value("1.3"));
    }

    @Test
    void unsetVersionsRequireNothing() throws Exception {
        mvc("", " ").perform(get("/app-version/ios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimum").doesNotExist())
                .andExpect(jsonPath("$.latest").doesNotExist());
    }
}
