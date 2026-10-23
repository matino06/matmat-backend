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
        return system("fizika", level);
    }

    private String system(String subject, String level) {
        when(plan.course(5)).thenReturn(new PlanSupport.CourseInfo(5, "Predmet", subject, level, 7));
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
        assertTrue(primary.contains("ne izvodi ih preuređivanjem"));
        assertFalse(primary.contains("begin{aligned}"));

        String secondary = system("ss");
        assertTrue(secondary.contains("cijeli zadatak do oko 120"));
        assertFalse(secondary.contains("Nikad ne traži sustav jednadžbi"));
        assertTrue(secondary.contains("Preuređivanje formule pokaži korak po korak"));
        assertTrue(secondary.contains("SKICE U OBJAŠNJENJU"));
    }

    @Test
    void chemistryPromptListsChemistrySketches() {
        assertTrue(system("kemija", "ss").contains("energijski_dijagram"));
        assertTrue(system("kemija", "ss").contains("titracija"));
        assertFalse(system("fizika", "ss").contains("energijski_dijagram"));
    }
}
