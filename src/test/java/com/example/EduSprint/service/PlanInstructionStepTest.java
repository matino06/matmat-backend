package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlanInstructionStepTest {

    private final PlanInstructionStep step = new PlanInstructionStep(null, null, null);

    @Test
    void keepsOnlyValidOperationsOnExistingLessons() throws Exception {
        ObjectNode out = step.sanitize(new ObjectMapper().readTree("""
                {"poruka":"Spajam dvije lekcije.","operacije":[
                  {"op":"spoji","a":1,"b":2},
                  {"op":"spoji","a":1,"b":1},
                  {"op":"spoji","a":1,"b":99},
                  {"op":"preimenuj","id":3,"naziv":"Kosina"},
                  {"op":"preimenuj","id":3,"naziv":" "},
                  {"op":"obrisi","id":99},
                  {"op":"dodaj","iza":null,"naziv":"Vektori"},
                  {"op":"dodaj","iza":42,"naziv":"Sile"},
                  {"op":"premjesti","id":2,"iza":2},
                  {"op":"premjesti","id":2,"iza":3},
                  {"op":"izmisli","id":1}
                ]}"""), Set.of(1L, 2L, 3L));
        assertEquals("Spajam dvije lekcije.", out.get("poruka").asText());
        assertEquals(4, out.get("operacije").size());
        assertEquals("spoji", out.get("operacije").get(0).get("op").asText());
        assertEquals("preimenuj", out.get("operacije").get(1).get("op").asText());
        assertEquals("dodaj", out.get("operacije").get(2).get("op").asText());
        assertEquals("premjesti", out.get("operacije").get(3).get("op").asText());
    }

    @Test
    void missingOperationsGiveAnEmptyProposal() throws Exception {
        ObjectNode out = step.sanitize(new ObjectMapper().readTree("{\"poruka\":\"Ne mogu.\"}"), Set.of(1L));
        assertEquals(0, out.get("operacije").size());
    }
}
