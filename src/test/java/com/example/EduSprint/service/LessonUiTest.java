package com.example.EduSprint.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Prompt i provjera interaktivne lekcije iz istog JS paketa kao na frontendu, u GraalJS-u.
class LessonUiTest {

    private static final LessonUi ui = new LessonUi();

    static final String VALID = """
            {"verzija":1,"zadatak":{"tekst":"Kamen u menzuri: voda je na 140 mL, a nakon uranjanja na 220 mL."},
             "podzadaci":[{"id":"a","naslov":"Volumen kamena",
               "pitanje":{"tip":"broj","tekst":"Koliki je volumen kamena?","odgovor":80,"jedinica":"cm³",
                          "krivo":[{"vrijednost":220,"poruka":"To je završno očitanje."}]},
               "zasto":"Kamen podigne vodu za svoj volumen: 220 − 140 = 80.","objasnjenje":"Volumen je razlika očitanja.",
               "pomoc":[{"tip":"pojam","tekst":"Uronjeno tijelo istisne vode koliko samo zauzima."},
                        {"tip":"broj","tekst":"Voda je s 100 mL porasla na 150 mL. Koliko je to?","odgovor":50,"jedinica":"mL"}],
               "blizanac":{"tip":"broj","tekst":"Voda je s 160 mL porasla na 250 mL. Koliki je volumen tijela?","odgovor":90,"jedinica":"cm³"}}],
             "zapamti":["Volumen tijela = završno − početno očitanje."]}""";

    @Test
    void validLessonPasses() {
        LessonUi.Check c = ui.check(VALID, "{\"a\":\"80 cm³\"}", null);
        assertEquals(List.of(), c.errors());
        assertTrue(c.ok());
    }

    @Test
    void lookStepWithAnswerAndUnknownWidgetAreRejected() {
        String bad = VALID.replace("{\"tip\":\"pojam\",\"tekst\":\"Uronjeno tijelo istisne vode koliko samo zauzima.\"}",
                "{\"tip\":\"pojam\",\"tekst\":\"x\",\"odgovor\":5},{\"tip\":\"zmaj\"}");
        List<String> errors = ui.check(bad, null, null).errors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("ne smije imati polje „odgovor“")), errors.toString());
        assertTrue(errors.stream().anyMatch(e -> e.contains("zmaj")), errors.toString());
        assertEquals(List.of("lekcija nije ispravan JSON"), ui.check("{", null, null).errors());
    }

    @Test
    void everySourceResultMustBeAnAnswer() {
        // litre samo u pomoći: većina učenika ih nikad ne izračuna
        List<String> errors = ui.check(VALID, "{\"a\":\"0,08 l\"}", null).errors();
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("0,08"), errors.toString());
        // u lancu jedinica dovoljna je jedna izračunata vrijednost; ostale su upozorenje
        LessonUi.Check chain = ui.check(VALID, "{\"a\":\"80 cm³ = 0,08 l\"}", null);
        assertTrue(chain.ok(), chain.errors().toString());
        assertTrue(chain.warnings().stream().anyMatch(w -> w.contains("0,08")), chain.warnings().toString());
        // obrazloženje iza „jer“ ili „;“ nije rezultat
        assertTrue(ui.check(VALID, "{\"a\":\"80 cm³, jer 220 − 140 = 80; razlika 2 očitanja\"}", null).ok());
    }

    @Test
    void sourceSketchMustStayVisible() {
        String figure = "{\"tip\":\"pravokutnik\",\"a\":4,\"b\":3,\"oznake\":{\"a\":\"4 dm\",\"b\":\"3 dm\"}}";
        List<String> errors = ui.check(VALID, null, figure).errors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("ne vidi lik")), errors.toString());
        // instrument smije postati pitanje „očitaj“ umjesto skice uz zadatak
        String scale = "{\"tip\":\"ljestvica\",\"instrument\":\"menzura\",\"od\":0,\"do\":300,\"razmak\":10,\"jedinica\":\"mL\",\"vrijednosti\":[140,220]}";
        assertFalse(ui.check(VALID, null, scale).ok());
        String reading = VALID.replace("{\"tip\":\"broj\",\"tekst\":\"Koliki je volumen kamena?\",\"odgovor\":80,\"jedinica\":\"cm³\",",
                "{\"tip\":\"mjerilo\",\"nacin\":\"procitaj\",\"instrument\":\"menzura\",\"od\":0,\"do\":300,\"razmak\":10,\"jedinica\":\"mL\",\"vrijednost\":80,\"tekst\":\"Očitaj.\",");
        assertEquals(List.of(), ui.check(reading, null, scale).errors());
    }

    @Test
    void studentIsAddressedInMasculine() {
        List<String> errors = ui.check(VALID.replace("Koliki je volumen kamena?", "Jesi li sam/sama izračunao?"), null, null).errors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("sam/sama")), errors.toString());
        errors = ui.check(VALID.replace("To je završno očitanje.", "Podijelila si krivo."), null, null).errors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("Podijelila si")), errors.toString());
        assertTrue(ui.check(VALID.replace("To je završno očitanje.", "Podijelio si krivo, 1 g/cm³."), null, null).ok());
    }

    @Test
    void twinNeedsADifferentResult() {
        String same = VALID.replace("porasla na 250 mL. Koliki je volumen tijela?\",\"odgovor\":90", "porasla na 240 mL. Koliki je volumen tijela?\",\"odgovor\":80");
        assertNotEquals(VALID, same);
        List<String> errors = ui.check(same, null, null).errors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("isti rezultat kao pitanje")), errors.toString());
        // faktor bez jedinice („10 puta manja“) smije ostati isti
        String factor = same.replace(",\"jedinica\":\"cm³\"}", "}");
        assertFalse(ui.check(factor, null, null).errors().stream().anyMatch(e -> e.contains("isti rezultat")));
    }

    @Test
    void practiceAndUnitChainsAreChecked() {
        String practice = VALID.replace("\"zapamti\":[", "\"vjezba\":{\"velicina\":\"duljina\",\"jedinice\":[\"m\",\"dm\",\"cm\",\"mm\"],\"lanac\":true,\"mjesovito\":true},\"zapamti\":[");
        assertEquals(List.of(), ui.check(practice, null, null).errors());
        List<String> errors = ui.check(practice.replace("\"mm\"]", "\"kg\"]"), null, null).errors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("vjezba.jedinice")), errors.toString());
        // lanac: točni rezultati računaju se u kodu, pa „8000“ vrijedi kao odgovor na rezultat
        String chain = VALID.replace("{\"tip\":\"broj\",\"tekst\":\"Koliki je volumen kamena?\",\"odgovor\":80,\"jedinica\":\"cm³\",",
                "{\"tip\":\"pretvorba_jedinica\",\"velicina\":\"duljina\",\"vrijednost\":8,\"od\":\"m\",\"do\":[\"dm\",\"cm\",\"mm\"],");
        assertNotEquals(VALID, chain);
        assertEquals(List.of(), ui.check(chain, "{\"a\":\"8 m = 8000 mm\"}", null).errors());
        assertTrue(ui.system().contains("vjezba"));
    }

    @Test
    void promptsComeFromTheCatalog() throws Exception {
        assertTrue(ui.system().contains("### mjerilo"));
        // widgeti za ručno pisane lekcije (matura) nisu u promptu za automatsku izradu
        assertFalse(ui.system().contains("### graf_funkcije"), "samoRucno widget u promptu");
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("subject", "fizika");
        source.put("level", "os78");
        source.put("objective_name", "Mjerenje volumena");
        source.put("task_text", "a) Očitaj …");
        source.put("results", "{\"a\":\"80 cm³\"}");
        source.put("explanation", "…");
        String user = ui.user(source);
        assertTrue(user.contains("7.–8. razred") && user.contains("Mjerenje volumena"), user);
        assertTrue(ui.repair(List.of("prva greška")).contains("- prva greška"));
    }
}
