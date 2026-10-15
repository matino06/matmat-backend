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
               "blizanac":{"tip":"broj","tekst":"Voda je s 160 mL porasla na 240 mL. Koliki je volumen tijela?","odgovor":80,"jedinica":"cm³"}}],
             "zapamti":["Volumen tijela = završno − početno očitanje."]}""";

    @Test
    void validLessonPasses() {
        LessonUi.Check c = ui.check(VALID, "{\"a\":\"80 cm³\"}");
        assertEquals(List.of(), c.errors());
        assertTrue(c.ok());
    }

    @Test
    void lookStepWithAnswerAndUnknownWidgetAreRejected() {
        String bad = VALID.replace("{\"tip\":\"pojam\",\"tekst\":\"Uronjeno tijelo istisne vode koliko samo zauzima.\"}",
                "{\"tip\":\"pojam\",\"tekst\":\"x\",\"odgovor\":5},{\"tip\":\"zmaj\"}");
        List<String> errors = ui.check(bad, null).errors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("ne smije imati polje „odgovor“")), errors.toString());
        assertTrue(errors.stream().anyMatch(e -> e.contains("zmaj")), errors.toString());
        assertEquals(List.of("lekcija nije ispravan JSON"), ui.check("{", null).errors());
    }

    @Test
    void promptsComeFromTheCatalog() throws Exception {
        assertTrue(ui.system().contains("### mjerilo"));
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
