package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VariationPromptTest {

    private final PlanSupport plan = mock(PlanSupport.class);
    private final VariationPrompt prompt = new VariationPrompt(mock(NamedParameterJdbcTemplate.class), plan);

    private String system(String level) {
        when(plan.course(5)).thenReturn(new PlanSupport.CourseInfo(5, "Fizika 7", "fizika", level, 7));
        when(plan.availableOnExam(5)).thenReturn("");
        when(plan.assumedKnowledge(any())).thenReturn(List.of("matematika do 6. razreda"));
        return prompt.system(5);
    }

    @Test
    void primarySchoolTasksAreShorterThanSecondary() {
        String primary = system("os78");
        assertTrue(primary.contains("cijeli zadatak do oko 90"));
        assertTrue(primary.contains("Nikad ne traži sustav jednadžbi"));
        assertTrue(primary.contains("predznanje: matematika do 6. razreda."));

        String secondary = system("ss");
        assertTrue(secondary.contains("cijeli zadatak do oko 120"));
        assertFalse(secondary.contains("Nikad ne traži sustav jednadžbi"));
    }
}
