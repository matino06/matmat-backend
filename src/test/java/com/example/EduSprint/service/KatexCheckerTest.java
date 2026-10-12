package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KatexCheckerTest {

    private final KatexChecker katex = new KatexChecker();

    @Test
    void validFormulasPassAndBrokenOnesAreReported() {
        assertTrue(katex.errors("Brzina \\( v = 20 \\ \\text{m/s} \\) i $$E = \\frac{mv^2}{2} = \\textcolor{green}{0{,}8 \\ \\text{J}}$$").isEmpty());
        var errors = katex.errors("Pokvareno: \\( \\frac{1}{2 \\)");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("\\frac{1}{2"));
    }
}
