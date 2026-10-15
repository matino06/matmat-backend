package com.example.EduSprint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Odgovori modela s LaTeX-om bez udvostručenog backslasha (tako je pala izrada materijala „Fizika Marijeina lekcija“).
class JsonLatexTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private String markdown(String json) throws Exception {
        return mapper.readTree(JsonLatex.fix(json)).path("markdown").asText();
    }

    @Test
    void invalidEscapesBecomeLiteralBackslashes() throws Exception {
        assertEquals("Gustoća \\(\\rho = \\frac{m}{V}\\) je omjer.", markdown("{\"markdown\":\"Gustoća \\(\\rho = \\frac{m}{V}\\) je omjer.\"}"));
        assertEquals("\\theta \\times 2 \\text{ cm}", markdown("{\"markdown\":\"\\theta \\times 2 \\text{ cm}\"}"));
    }

    @Test
    void correctJsonIsUnchanged() throws Exception {
        String ok = "{\"markdown\":\"Prvi red\\nDrugi \\\"citat\\\" \\\\frac{1}{2} \\u0161ef\",\"vrsta\":\"tekst\"}";
        assertEquals(ok, JsonLatex.fix(ok));
        assertEquals("Prvi red\nDrugi \"citat\" \\frac{1}{2} šef", markdown(ok));
    }

    @Test
    void newlineBeforeAWordStaysANewline() throws Exception {
        assertEquals("Mjeri\nu tablici", markdown("{\"markdown\":\"Mjeri\\nu tablici\"}"));
        assertEquals("a\tb", markdown("{\"markdown\":\"a\\tb\"}"));
    }
}
