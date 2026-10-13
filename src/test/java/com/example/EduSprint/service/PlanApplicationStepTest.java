package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanApplicationStepTest {

    private static final Map<Long, Integer> POSITIONS = Map.of(11L, 1, 12L, 2, 13L, 3);

    @Test
    void applicationLessonGoesAfterTheLastLessonItUses() throws Exception {
        List<PlanApplicationStep.Application> out = PlanApplicationStep.parse(new ObjectMapper().readTree("""
                {"lekcije_primjene":[
                  {"naziv":"Kosina s trenjem","tip":"Kombinacija","koristi":[13,11,13],"opis":"o","primjer_situacije":"p"},
                  {"naziv":"Graf v(t)","tip":"graf","koristi":[99,12]}
                ]}"""), POSITIONS);
        assertEquals(2, out.size());
        assertEquals("kombinacija", out.get(0).type());
        assertEquals(List.of(13L, 11L), out.get(0).uses());
        assertEquals(13L, out.get(0).after());
        assertEquals(List.of(12L), out.get(1).uses());
        assertEquals(12L, out.get(1).after());
    }

    @Test
    void unknownTypesAndLessonsWithoutKnownPrerequisitesAreDropped() throws Exception {
        List<PlanApplicationStep.Application> out = PlanApplicationStep.parse(new ObjectMapper().readTree("""
                {"lekcije_primjene":[
                  {"naziv":"X","tip":"vježba","koristi":[11]},
                  {"naziv":"Y","tip":"zamka","koristi":[99]},
                  {"naziv":" ","tip":"zamka","koristi":[11]}
                ]}"""), POSITIONS);
        assertTrue(out.isEmpty());
    }

    @Test
    void materialQuestionsAreTakenFromTheirSection() {
        String page = "Gustoća je svojstvo tvari.\nNAUČILI SMO\nGustoća je količnik mase i volumena.\n\nPitanja i zadatci\n"
                + "1. Odredi gustoću svog tijela.\n2. Izračunaj u kojem slučaju nosiš veću masu: 5 litara vode ili 5 litara ulja.";
        List<String> parts = PlanApplicationStep.taskParts(page);
        assertEquals(1, parts.size());
        assertTrue(parts.get(0).startsWith("Pitanja i zadatci"));
        assertTrue(parts.get(0).contains("5 litara ulja"));
        assertTrue(PlanApplicationStep.taskParts("Samo tekst bez pitanja.").isEmpty());
    }
}
