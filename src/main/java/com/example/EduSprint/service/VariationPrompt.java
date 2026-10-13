package com.example.EduSprint.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// Prompt za 1. varijaciju zadatka lekcije (prototip sol1_write.py: DESIGN_V2 + česte greške), poopćen na sve
// predmete. Zajednički dio (pravila, predmet, materijal dostupan na ispitu) je u system poruci i jednak za sve
// lekcije predmeta, pa se kešira; lekcija je u user poruci.
@Component
public class VariationPrompt {

    static final String SYSTEM = """
            Pišeš zadatak za MatMat, aplikaciju u kojoj učenik uči kroz zadatke s laičkim objašnjenjima. Predmet: %s.

            KAKO NAPRAVITI ZADATAK KOJI UČI:
            - Zadatak uči gradivo lekcije; učenik ga ne zna unaprijed i uči ga iz objašnjenja. Nema uvodnog teksta.
            - Jedna realna situacija kroz NAJVIŠE TRI podzadatka a), b), c), nikad d): podzadaci se nastavljaju jedan na drugi.
            - Podzadaci zajedno pokrivaju sve stavke lekcije. Barem jedan traži razumijevanje (koliko puta se nešto
              promijeni, što ako, zašto) i ugrađen je prirodno u situaciju, ne kao „Navedi…“ ili „Opiši…“.
            - Ne otkrivaj formulu u tekstu zadatka. Ne prepisuj zadatke s ispita.
            - Uzmi lijepe brojeve s kojima se lako računa; rezultati moraju biti realni.%s%s
            - Koristi samo stavke ove lekcije, ono što učenik već zna i predznanje: %s.
            - Nikad ne crtaj graf, shemu ni sliku znakovima u tekstu (crtice, kose crte, točkice, blok koda): učenik to ne
              može pouzdano očitati. Ako zadatak treba graf ili crtež, zadaj podatke tablicom ili riječima (npr. „pravac prolazi
              ishodištem i točkom (20 cm³, 60 g)“).

            KAKO OBJAŠNJAVATI:
            - Jedan korak po podzadatku: `## <emoji> Kratak naslov`, pa odmah **podebljano ponovljeno pitanje
              podzadatka**, pa jednom rečenicom što je zadano.
            - Novi pojam uvedi tek kad ga podzadatak treba: prvo što on JEST svakodnevnim jezikom, s usporedbom iz
              života, i zašto se tako zove. Tek onda formula.
            %s
            - Preuređivanje formule pokaži korak po korak s operacijom desno:
              $$\\begin{aligned} pV &= nRT && / : V \\\\ p &= \\frac{nRT}{V} \\end{aligned}$$
            - Pretvorbe jedinica napiši izričito prije uvrštavanja; uvrštavanje pokaži s brojevima; konačan rezultat
              \\textcolor{green}{…}.
            - Piši u odlomcima od 2–4 povezane rečenice, prirodnim tonom („Primijeti…“, „Ključna riječ u zadatku je
              **jednoliko**.“). Ne piši telegrafski.
            - Zamku najavi izričito („Prvo jedna zamka.“) i objasni zašto je zamka.
            - Nakon rezultata jednom ili dvjema rečenicama poveži ga s intuicijom ili provjeri drugim putem.
            - Pojmove i ključne riječi podebljaj. Decimalni zarez u LaTeX-u: 0{,}8. Jedinice: 20 \\ \\text{m/s}.
            - LaTeX: inline \\( … \\), display $$ … $$ u jednom retku (osim aligned), prazan red prije i poslije.
            - Zadnji korak `## ✅ Rezultati`: po jedan redak za svaki podzadatak s rezultatom.

            ČESTE GREŠKE KOJE NE SMIJEŠ NAPRAVITI:
            - Nikakve samokorekcije u tekstu („26 N? Ne, …“). Objašnjenje je konačna verzija.
            - Ne tvrdi što piše ili ne piše u materijalu dostupnom na ispitu osim ako to doslovno vidiš u priloženom tekstu.
            - Svaka slikovita usporedba i „intuicija“ mora biti stručno točna.
            - Podaci u podzadacima ne smiju proturječiti jedni drugima; ako podzadatak mijenja uvjet, izričito reci
              vrijedi li promjena i u sljedećim podzadacima.%s
            - Formule piši samo unutar \\( \\) ili $$ $$; izvan njih nikad LaTeX naredbe ni {,}.
            - Zadnji odjeljak „## ✅ Rezultati“ s po jednim retkom za svaki podzadatak je obavezan.

            Odgovori isključivo kao json objekt {"zadatak":"markdown teksta zadatka","objasnjenje":"markdown objašnjenja",
            "rezultati":{"a":"konačan odgovor","b":"…"}}. U rezultatima za računske podzadatke broj s jedinicom, za ostale jedna rečenica.

            %s""";

    static final String FORMULA_FROM_MATERIAL = """
            - Formulu ne izmišljaj iz zraka: reci gdje je — „u **<naziv materijala>, str. N**“ — i prepiši je kao display
              formulu. Konstante isto tako. Ako formule nema u materijalu, izvedi je u par redaka iz one koja jest i reci
              „Nema je u materijalu, ali se iz njega izvede u dva reda“.""";
    static final String FORMULA_WITHOUT_MATERIAL = """
            - Učenik na ispitu nema materijal s formulama: formulu uvedi iz onoga što zna, objasni odakle dolazi i kako je zapamtiti.""";
    static final String PHYSICS_NUMBERS = " Uzmi g = 10 m/s².";
    // Matematika koju učenik 7.–8. razreda zna: računa se korak po korak, jednom nepoznanicom.
    static final String PRIMARY_MATH = """

            - Učenik je u 7. ili 8. razredu osnovne škole: računaj samo osnovnim računskim operacijama, omjerima i jednostavnim
              jednadžbama s jednom nepoznanicom. Nikad ne traži sustav jednadžbi, dvije nepoznanice ni množenje zagrada; ako
              zadatak to traži, promijeni ga tako da se rješava korak po korak.""";
    static final String PHYSICS_MISTAKES = """

            - Težina NIJE sila teža: sila teža je sila kojom Zemlja privlači tijelo, težina je sila kojom tijelo
              djeluje na podlogu ili ovjes.
            - Boje: 600 nm je narančasto, crveno je ~620–750 nm, zeleno ~500–565 nm, plavo ~450–490 nm.
            - Energija kondenzatora je CU²/2; izvor pri punjenju uloži CU².""";

    // Varijacije 2–4 po predlošku 1. varijacije (prototip sablon_varijacije.py).
    static final String TEMPLATE_SYSTEM = """
            Pišeš zadatak za MatMat, aplikaciju u kojoj učenik uči kroz zadatke s laičkim objašnjenjima. Predmet: %s.
            Dobit ćeš PREDLOŽAK: provjereno točan zadatak s objašnjenjem. Napravi NOVU VARIJACIJU istog zadatka.

            PRAVILA:
            - Ista razina težine, isti podzadaci, isti postupak i jednako detaljno objašnjenje kao predložak. Zadrži strukturu,
              naslove, analogije, citate materijala i stil. Najviše tri podzadatka a), b), c), kao u predlošku.
            - Promijeni zadane brojeve i sitnice priče (npr. lijevo/desno, gore/dolje, kut, druga tijela ili imena), tako da
              učenik ne može prepisati rezultate iz prethodnih varijacija.
            - Sve brojeve izračunaj ponovno i pažljivo, uključujući međurezultate.
            - Svaku rečenicu koja ovisi o brojevima ili o promijenjenoj sitnici (usporedbe, postotci, smjer, predznak, koja je
              veličina veća …) prilagodi ili ukloni tako da ostane istinita.
            - Novi podaci moraju biti realni i dati smislene rezultate.
            - Nemoj ponoviti vrijednosti iz već napravljenih varijacija.

            ČESTE GREŠKE KOJE NE SMIJEŠ NAPRAVITI:
            - Nikakve samokorekcije u tekstu. Objašnjenje je konačna verzija.
            - Podaci u podzadacima ne smiju proturječiti jedni drugima.%s
            - Formule piši samo unutar \\( \\) ili $$ $$; izvan njih nikad LaTeX naredbe ni {,}.
            - Nikad ne crtaj graf ni sliku znakovima u tekstu; podatke zadaj tablicom ili riječima.
            - Zadnji odjeljak „## ✅ Rezultati“ s po jednim retkom za svaki podzadatak je obavezan.

            Vrati json {"zadatak":"markdown teksta zadatka","objasnjenje":"markdown objašnjenja","rezultati":{"a":"konačan odgovor","b":"…"}}.""";

    private final NamedParameterJdbcTemplate jdbc;
    private final PlanSupport plan;

    public VariationPrompt(NamedParameterJdbcTemplate jdbc, PlanSupport plan) {
        this.jdbc = jdbc;
        this.plan = plan;
    }

    public String system(long courseId) {
        PlanSupport.CourseInfo course = plan.course(courseId);
        String material = plan.availableOnExam(courseId);
        boolean physics = "fizika".equals(course.subject());
        return SYSTEM.formatted(course.describe(), physics ? PHYSICS_NUMBERS : "", "os78".equals(course.level()) ? PRIMARY_MATH : "",
                String.join("; ", plan.assumedKnowledge(course)),
                material.isEmpty() ? FORMULA_WITHOUT_MATERIAL : FORMULA_FROM_MATERIAL,
                physics ? PHYSICS_MISTAKES : "", material);
    }

    public String templateSystem(long courseId) {
        PlanSupport.CourseInfo course = plan.course(courseId);
        return TEMPLATE_SYSTEM.formatted(course.describe(), "fizika".equals(course.subject()) ? PHYSICS_MISTAKES : "");
    }

    // Predložak (1. varijacija) i zadaci već napravljenih varijacija, da se vrijednosti ne ponove.
    public String templateUser(String template, List<String> previous) {
        StringBuilder sb = new StringBuilder("PREDLOŽAK:\n").append(template);
        if (!previous.isEmpty()) {
            sb.append("\n\nVEĆ NAPRAVLJENE VARIJACIJE (njihovi zadaci, ne ponavljaj te vrijednosti):\n").append(String.join("\n\n", previous));
        }
        return sb.toString();
    }

    // Lekcija: naziv i stavke (ili tip, opis i primjer situacije za lekciju primjene) te stavke izravnih preduvjeta.
    public String user(long objectiveId) {
        MapSqlParameterSource o = new MapSqlParameterSource("o", objectiveId);
        Map<String, Object> l = jdbc.queryForMap(
                "SELECT objective_name, concept, kind, application_type, example_situation FROM learning_objective WHERE objective_id = :o", o);
        List<String> items = jdbc.queryForList(
                "SELECT text FROM knowledge_item WHERE objective_id = :o AND duplicate_of IS NULL ORDER BY position", o, String.class);
        List<String> known = jdbc.queryForList("""
                SELECT k.text FROM objective_prerequisite p JOIN knowledge_item k ON k.objective_id = p.prerequisite_id
                WHERE p.objective_id = :o AND k.duplicate_of IS NULL ORDER BY p.prerequisite_id, k.position""", o, String.class);
        StringBuilder sb = new StringBuilder();
        if (PlanApplicationStep.LESSON_KIND.equals(l.get("kind"))) {
            sb.append("LEKCIJA PRIMJENE (tip zadatka: ").append(l.get("application_type")).append("): ").append(l.get("objective_name"))
                    .append("\n\nŠTO UČI: ").append(l.get("concept"))
                    .append("\n\nPRIMJER SITUACIJE (uzmi ovu ili sličnu): ").append(l.get("example_situation"))
                    .append("\n\nLekcija ne uvodi novo gradivo: zadatak uvježbava ovaj tip zadatka na gradivu koje učenik već zna.");
        } else {
            sb.append("LEKCIJA: ").append(l.get("objective_name")).append("\n\nSTAVKE OVE LEKCIJE (to uči):\n");
            items.forEach(i -> sb.append("- ").append(i).append('\n'));
        }
        sb.append("\n\nUČENIK VEĆ ZNA (stavke preduvjeta):\n");
        if (known.isEmpty()) sb.append("—");
        known.forEach(k -> sb.append("- ").append(k).append('\n'));
        return sb.toString();
    }
}
