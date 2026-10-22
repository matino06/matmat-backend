package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Isti JS kao na frontendu, u GraalJS-u: ispravan opis prolazi, krivi brojevi i nemogući podaci ne.
class SketchCheckerTest {

    private static final SketchChecker checker = new SketchChecker();

    @Test
    void validScaleAndCuboidPass() {
        String text = "Volumen kamena: razina 48 mL prije i 68 mL poslije uranjanja. Posuda 25 cm i 16 cm, visina vode 8 cm.";
        assertEquals(List.of(), checker.problems("""
                {"tip":"ljestvica","instrument":"menzura","od":40,"do":80,"razmak":4,"oznaka_svakih":20,"jedinica":"mL","vrijednosti":[48,68]}""", text));
        assertEquals(List.of(), checker.problems("""
                {"tip":"kvadar","a":25,"b":16,"c":20,"razina":8,"oznake":{"a":"25 cm","b":"16 cm","razina":"h = ?"}}""", text));
    }

    @Test
    void wrongNumbersImpossibleShapesAndBadJsonAreRejected() {
        String text = "Pravokutni trokut s katetama a = 6 cm i b = 8 cm.";
        List<String> p = checker.problems("""
                {"tip":"trokut","stranice":{"a":6,"b":7,"c":10},"pravi_kut":"C","oznake":{"a":"6 cm","b":"7 cm","c":"?"}}""", text);
        assertTrue(p.stream().anyMatch(x -> x.contains("pravi kut")), p.toString());
        assertTrue(p.stream().anyMatch(x -> x.contains("broj 7")), p.toString());
        assertFalse(checker.problems("""
                {"tip":"ljestvica","instrument":"vaga","od":0,"do":1000,"razmak":30,"vrijednosti":[540]}""", "540 g").isEmpty());
        assertFalse(checker.problems("""
                {"tip":"graf","x":[0,40],"y":[0,120],"funkcije":[{"izraz":"3*x"}],"tocke":[{"x":20,"y":50}]}""", "").isEmpty());
        assertEquals(List.of("opis skice nije ispravan JSON"), checker.problems("{tip:", ""));
        assertFalse(checker.problems("{\"tip\":\"zmaj\"}", "").isEmpty());
    }

    @Test
    void graphExpressionCannotRunArbitraryCode() {
        assertFalse(checker.problems("""
                {"tip":"graf","x":[0,1],"y":[0,1],"funkcije":[{"izraz":"x;globalThis.y=1"}],"tocke":[]}""", "").isEmpty());
    }

    @Test
    void tooDenseScaleIsRejected() {
        List<String> p = checker.problems("""
                {"tip":"ljestvica","instrument":"menzura","od":0,"do":100,"razmak":2,"oznaka_svakih":20,"jedinica":"mL","vrijednosti":[60,80]}""",
                "60 mL i 80 mL");
        assertTrue(p.stream().anyMatch(x -> x.contains("podjeljaka")), p.toString());
    }

    @Test
    void chemistrySketchesCheckTheirChemistry() {
        String text = "CH₄ + 2 O₂ → CO₂ + 2 H₂O, ΔH = −891 kJ/mol.";
        assertEquals(List.of(), checker.problems("""
                {"tip":"energijski_dijagram","razine":[{"oznaka":"CH₄ + 2 O₂","vrijednost":-75},{"oznaka":"CO₂ + 2 H₂O","vrijednost":-966}],
                "prijelazi":[{"od":0,"do":1,"oznaka":"ΔH = −891 kJ/mol"}]}""", text));
        List<String> sign = checker.problems("""
                {"tip":"energijski_dijagram","razine":[{"vrijednost":-75},{"vrijednost":-966}],"prijelazi":[{"od":0,"do":1,"oznaka":"ΔH = +891 kJ/mol"}]}""", "+891");
        assertTrue(sign.stream().anyMatch(x -> x.contains("NE odgovara")), sign.toString());
        // ugljikov(IV) oksid s jednostrukim vezama nema oktet
        List<String> octet = checker.problems("""
                {"tip":"strukturna_formula","atomi":[{"id":"C","simbol":"C","x":0,"y":0,"nevezni":0},{"id":"O1","simbol":"O","x":-1,"y":0,"nevezni":2},
                {"id":"O2","simbol":"O","x":1,"y":0,"nevezni":2}],"veze":[["C","O1",1],["C","O2",1]]}""", "");
        assertTrue(octet.stream().anyMatch(x -> x.contains("NIJE popunjena")), octet.toString());
        List<String> atoms = checker.problems("""
                {"tip":"cestice","ocuvanje":true,"posude":[{"cestice":[{"vrsta":"H₂","broj":2},{"vrsta":"O₂","broj":1}]},
                {"cestice":[{"vrsta":"H₂O","broj":1}]}]}""", "");
        assertTrue(atoms.stream().anyMatch(x -> x.contains("NIJE jednako")), atoms.toString());
        assertEquals(List.of(), checker.problems("""
                {"tip":"titracija","kiselina":{"formula":"HCl","c":0.1,"V":20},"luzina":{"c":0.1},"indikator":"fenolftalein"}""", "utrošeno 20 mL"));
    }
}
