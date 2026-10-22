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
            - Jedna kratka realna situacija kroz NAJVIŠE TRI podzadatka a), b), c), nikad d): podzadaci se nastavljaju jedan na drugi.
            - Zadatak i objašnjenje zajedno pokrivaju sve stavke lekcije. Stavka s postupkom ili razumijevanjem dobiva pitanje;
              stavku koja je samo činjenica (npr. „postoje različite vrste vaga“) smiješ naučiti samo u odjeljku „📌 Zapamti“.
              Kad sve stavke ne stanu u tri podzadatka, najprije odredi koje materijal ističe: one iz sažetka („Naučili
              smo“) i one o kojima ima pitanje ili zadatak. One dolaze na test i dobivaju pitanje; u „Zapamti“ ide stavka
              koje u sažetku i pitanjima nema, čak i kad je prva u lekciji.
            - Barem jedan podzadatak traži razumijevanje (koliko puta se nešto promijeni, što ako, zašto) i ugrađen je
              prirodno u situaciju, ne kao „Navedi…“ ili „Opiši…“.
            - Kratko: učenik mora pročitati sve, a dug tekst ga odbija i ne uči ništa više. Svaki podzadatak ima jedno glavno
              pitanje, najviše uz jedno kratko „zašto“. Situaciju opiši jednom rečenicom i samo podacima koji trebaju za
              rješavanje: bez imena, priče i ukrasnih detalja koji ništa ne uče. Svi podzadaci su u istoj situaciji; ne
              spajaj nepovezane stvari (putnik u autu i čokolada). Isti postupak ne traži dvaput (dvije
              površine, sedam jedinica: dovoljna je jedna površina i dvije jedinice). Ne dodaj ograde za slučajeve na koje
              učenik ne bi ni pomislio („ništa se ne gubi“, „pri istom tlaku“).
            - Kraće ne znači opisno: postupak koji lekcija uči (račun, pretvaranje, očitavanje, prebrojavanje) učenik izvodi
              s brojevima. Pitanje „Kako bi odredio…?“ ne zamjenjuje zadatak u kojem to stvarno odredi.
            - Ne otkrivaj formulu u tekstu zadatka. Ne prepisuj zadatke s ispita.
            - Uzmi lijepe brojeve s kojima se lako računa; rezultati moraju biti realni.%s%s%s
            - Koristi samo stavke ove lekcije, ono što učenik već zna i predznanje: %s.
            - Ako je uz lekciju priloženo GRADIVO IZ MATERIJALA, učenik se iz njega sprema za test. Računski podzadatak smije
              tražiti samo postupak koji materijal objašnjava ili uvježbava (objašnjenjem, riješenim primjerom ili pitanjima i
              zadatcima). Što materijal samo spomene, bez postupka (npr. „gustoćom se može odrediti udio tvari u smjesi“), smiješ
              pitati samo riječima, nikad računom. Težina neka bude kao pitanja i zadatci u materijalu.
            - Nikad ne crtaj graf, shemu ni sliku znakovima u tekstu (crtice, kose crte, točkice, blok koda): učenik to ne
              može pouzdano očitati. Ako zadatak treba graf ili crtež, opiši ga u polju "skica" (vidi SKICA); jedini blok
              koda koji smiješ napisati je blok ```skica u objašnjenju (vidi SKICE U OBJAŠNJENJU).

            KAKO OBJAŠNJAVATI:
            - Jedan korak po podzadatku: `## <emoji> Kratak naslov`, pa odmah **podebljano ponovljeno pitanje
              podzadatka**. Ne prepričavaj što je zadano: učenik je to upravo pročitao.
            - Novi pojam uvedi tek kad ga podzadatak treba, jednom ili dvjema rečenicama: prvo što on JEST svakodnevnim
              jezikom, s usporedbom iz života, i zašto se tako zove. Tek onda formula. Pojam iz ranijih lekcija ne objašnjavaj ponovno.
            - Svaki korak objašnjava samo svoj podzadatak. Stavke lekcije koje zadatak ne pita ne stavljaj u korake, nego
              prije Rezultata u odjeljak `## 📌 Zapamti` s 2–4 kratke natuknice; ako ih nema, izostavi taj odjeljak. U
              „Zapamti“ idu samo stavke ove lekcije, ne gradivo drugih lekcija s istih stranica materijala.
            %s%s
            - Pretvorbe jedinica napiši izričito prije uvrštavanja; uvrštavanje pokaži s brojevima; konačan rezultat
              \\textcolor{green}{…}.
            - Piši kratkim odlomcima prirodnim tonom („Ključna riječ u zadatku je **jednoliko**.“), bez telegrafskog stila,
              ali i bez rečenica koje ponavljaju ono što piše u formuli ili u zadatku.
            - Zamku spomeni samo kad stvarno postoji (česta greška učenika baš u tom koraku): najavi je („Pazi:“) i objasni
              zašto je zamka. Ne izmišljaj zamku samo da bi je bilo.
            - Nakon rezultata dodaj provjeru ili intuiciju samo kad učeniku otkriva nešto novo (zašto je rezultat manji, gdje bi
              pogriješio); inače odmah idi na sljedeći podzadatak.
            - Podebljaj samo nove pojmove i ključnu riječ zadatka, najviše oko pet u cijelom objašnjenju: kad je podebljano
              sve, ništa se ne ističe. Decimalni zarez u LaTeX-u: 0{,}8. Jedinice: 20 \\ \\text{m/s}.
            - LaTeX: inline \\( … \\), display $$ … $$ u jednom retku (osim aligned), prazan red prije i poslije.
            - Zadnji korak `## ✅ Rezultati`: po jedan redak za svaki podzadatak s rezultatom.

            SKICE U OBJAŠNJENJU:
            - U objašnjenje umetni jednu ili dvije skice kad crtež pokazuje ideju koraka bolje od riječi: pravokutnik podijeljen
              na kvadratiće uz A = a · b, kvadar s dimenzijama uz V = a · b · c, menzura prije i poslije uranjanja, vaga,
              graf, trokut ili sile. Za učenike osnovne škole crtež je često ono što ideju učini jasnom: kad god neki tip
              skice pomaže, upotrijebi ga.
            - Skica u objašnjenju smije pokazati i rješenje (npr. mrežu kvadratića koje se broje). Ne ponavljaj skicu zadatka.
            - Ne uvodi prikaz koji učenik ne zna iz gradiva: graf samo ako ga lekcija ili materijal koristi (u 7. razredu
              fizike grafova nema).
            - Piše se kao zaseban blok na mjestu gdje pomaže, s JSON opisom u jednom retku, istim tipovima kao u SKICA:
              ```skica
              {"tip":"pravokutnik","a":6,"b":4,"mreza":1,"oznake":{"a":"6 cm","b":"4 cm"}}
              ```
              Brojevi u ovom primjeru samo pokazuju oblik. Brojevi s oznaka skice moraju se pojaviti u tekstu objašnjenja.

            ČESTE GREŠKE KOJE NE SMIJEŠ NAPRAVITI:
            - Nikakve samokorekcije u tekstu („26 N? Ne, …“). Objašnjenje je konačna verzija.
            - Ne tvrdi što piše ili ne piše u materijalu dostupnom na ispitu osim ako to doslovno vidiš u priloženom tekstu.
            - Svaka slikovita usporedba i „intuicija“ mora biti stručno točna.
            - Podaci u podzadacima ne smiju proturječiti jedni drugima; ako podzadatak mijenja uvjet, izričito reci
              vrijedi li promjena i u sljedećim podzadacima.%s
            - Formule piši samo unutar \\( \\) ili $$ $$; izvan njih nikad LaTeX naredbe ni {,}.
            - Zadnji odjeljak „## ✅ Rezultati“ s po jednim retkom za svaki podzadatak je obavezan.

            SKICA:
            - Ako crtež učeniku pomaže razumjeti ili riješiti zadatak (graf, ljestvica mjernog instrumenta koju treba očitati,
              tijelo ili lik s dimenzijama, sile na tijelo, strujni krug), dodaj polje "skica" s opisom JEDNOG crteža; inače
              "skica": null. Ne crtaš ti: aplikacija iz opisa sama nacrta crtež i provjeri ga.
            - Brojevi u skici moraju odgovarati zadatku i rješenju. Ako učenik nešto treba očitati sa skice, ne piši tu
              vrijednost u tekst zadatka, nego se pozovi na sliku („Očitaj sa slike…“); u objašnjenju je navedi.
            - Skica prikazuje samo zadane podatke i ono što učenik treba očitati, nikad ono što treba izračunati ili prebrojiti:
              npr. ne dijeli plohu na pločice kad se traži broj pločica, ne crtaj razinu vode kad se traži njezina visina
              (tada „razina": null), ne označavaj točku čije koordinate učenik računa.
            - Brojevi u primjerima ispod samo pokazuju oblik JSON-a. Nikad ih ne prepisuj u zadatak ni skicu: zadatak smisli sa
              svojim brojevima, a skica ih prati.
            - Tipovi (točno ovakav JSON; null gdje nešto ne treba):
              graf: {"tip":"graf","x":[0,70],"y":[0,140],"os_x":"V (cm³)","os_y":"m (g)","funkcije":[{"izraz":"1.7*x","oznaka":"A"}],
                "tocke":[{"x":30,"y":51,"oznaka":"(30, 51)","na":"A","zadana":true}]} (izraz: x, brojevi, + - * / ^ i zagrade;
                točke moraju ležati na funkciji; "zadana" ako je točka u tekstu zadatka)
              koordinatni: {"tip":"koordinatni","x":[-6,7],"y":[-2,9],"tocke":[{"oznaka":"A","x":-5,"y":3,"zadana":true}],
                "duzine":[["A","B"]],"polovista":[{"tocka":"M","od":["A","B"]}]}
              trokut: {"tip":"trokut","stranice":{"a":9,"b":12,"c":15},"pravi_kut":"C","oznake":{"a":"9 cm","b":"12 cm","c":"?"},"kutovi":{"α":"?"}}
              kruznica: {"tip":"kruznica","r":13,"tetiva":{"duljina":24},"oznake":{"r":"13 cm","tetiva":"24 cm","d":"?"}}
              pravokutnik: {"tip":"pravokutnik","a":270,"b":150,"mreza":null,"oznake":{"a":"2,7 m","b":"150 cm"}} (a, b i mreza u istoj
                jedinici; mreza = stranica jednog kvadratića, samo kad mreža nije ono što se traži)
              kvadar: {"tip":"kvadar","a":37,"b":19,"c":23,"razina":11,"oznake":{"a":"37 cm","b":"19 cm","c":"23 cm","razina":"11 cm"}}
                (razina = visina tekućine u kvadru, ili null)
              ljestvica: {"tip":"ljestvica","instrument":"menzura","od":0,"do":250,"razmak":5,"oznaka_svakih":50,"jedinica":"mL",
                "vrijednosti":[135,170]} (instrument: menzura, vaga ili ravnalo; razmak = vrijednost najmanjeg podjeljka;
                menzura: razina vode, ili dvije razine prije i poslije uranjanja; vaga: položaj kazaljke; ravnalo: početak i kraj predmeta;
                najviše 30 podjeljaka na menzuri, 60 na ravnalu i 90 na vagi; barem jedna vrijednost neka ne bude na označenoj crtici,
                da učenik vježba očitavanje podjeljaka)
              sile: {"tip":"sile","podloga":{"vrsta":"kosina","kut":37},"sile":[{"oznaka":"G","smjer":"dolje","iznos":65,"zadana":false}],
                "rastav":"G"} (vrsta: kosina ili vodoravna; smjer: dolje, gore, okomito_od_podloge, uz_kosinu, niz_kosinu, desno, lijevo)
              strujni_krug: {"tip":"strujni_krug","izvor":{"oznaka":"U = 9 V"},"spoj":{"serijski":[{"R":"R₁ = 7 Ω"},
                {"paralelno":[{"R":"R₂ = 15 Ω"},{"R":"R₃ = 10 Ω"}]}]},"instrumenti":[{"vrsta":"A"}]}%s

            Odgovori isključivo kao json objekt {"zadatak":"markdown teksta zadatka","objasnjenje":"markdown objašnjenja",
            "rezultati":{"a":"konačan odgovor","b":"…"},"skica":opis skice ili null}. U rezultatima za računske podzadatke broj
            s jedinicom, za ostale jedna rečenica.

            %s""";

    static final String FORMULA_FROM_MATERIAL = """
            - Formulu ne izmišljaj iz zraka: reci gdje je — „u **<naziv materijala>, str. N**“ — i prepiši je kao display
              formulu. Konstante isto tako. Ako formule nema u materijalu, izvedi je u par redaka iz one koja jest i reci
              „Nema je u materijalu, ali se iz njega izvede u dva reda“.""";
    static final String FORMULA_WITHOUT_MATERIAL = """
            - Učenik na ispitu nema materijal s formulama: formulu uvedi iz onoga što zna, objasni odakle dolazi i kako je zapamtiti.""";
    static final String PHYSICS_NUMBERS = " Uzmi g = 10 m/s².";
    static final String FORMULA_DERIVATION = """

            - Preuređivanje formule pokaži korak po korak s operacijom desno:
              $$\\begin{aligned} pV &= nRT && / : V \\\\ p &= \\frac{nRT}{V} \\end{aligned}$$""";
    // Učenik 7.–8. razreda formulu uzima iz udžbenika gotovu; preuređivanje i oznake iz srednje škole ga gube.
    static final String FORMULA_TEXTBOOK = """

            - Formule piši u obliku iz udžbenika (npr. m = ρV i V = m/ρ uz ρ = m/V) i ne izvodi ih preuređivanjem. Ne
              koristi oznake koje udžbenik ne koristi (npr. Δ za razliku): piši riječima „razlika“.""";
    // Duljina po razini: zadatak se čita cijeli, a objašnjenje bez ponavljanja uči jednako dobro.
    static final String LENGTH_PRIMARY = """

            - Duljina: podzadatak do oko 30 riječi, cijeli zadatak do oko 90; objašnjenje oko 350–450 riječi.""";
    static final String LENGTH_SECONDARY = """

            - Duljina: podzadatak do oko 40 riječi, cijeli zadatak do oko 120; objašnjenje oko 450–600 riječi.""";
    // Matematika koju učenik 7.–8. razreda zna: računa se korak po korak, jednom nepoznanicom.
    static final String PRIMARY_MATH = """

            - Učenik je u 7. ili 8. razredu osnovne škole: računaj samo osnovnim računskim operacijama, omjerima i jednostavnim
              jednadžbama s jednom nepoznanicom. Nikad ne traži sustav jednadžbi, dvije nepoznanice ni množenje zagrada; ako
              zadatak to traži, promijeni ga tako da se rješava korak po korak. Težina neka bude niska ili srednja, kao zadaci
              u školskom udžbeniku za taj razred.
            - Brojevi neka se mogu izračunati napamet ili na papiru, bez kalkulatora: izbjegavaj dijeljenje decimalnim brojem
              (umjesto 50 kg : 0,05 m³ uzmi 50 000 g i 50 000 cm³).
            - Najviše jedan podzadatak traži objašnjenje „zašto“; ostali traže broj ili kratak odgovor. Objasniti „zašto“
              prosječnom je učeniku najteže i najzamornije.""";
    // Kemijske skice (taskSketch.js crta ih i provjerava: oktet i naboj, razlike energija, očuvanje atoma, pH krivulje).
    static final String CHEMISTRY_SKETCHES = """

              U kemiji crtež često uči više od riječi: entalpijski dijagram, Lewisova ili strukturna formula, članak ili
              elektroliza, ljuske atoma, čestice prije i poslije reakcije, kromatogram, titracijska krivulja.
              energijski_dijagram: {"tip":"energijski_dijagram","os":"H","razine":[{"oznaka":"CH₄(g) + 2 O₂(g)","vrijednost":-75},
                {"oznaka":"CO₂(g) + 2 H₂O(ℓ)","vrijednost":-966}],"prijelazi":[{"od":0,"do":1,"oznaka":"ΔᵣH = −891 kJ/mol"}]}
                (razine slijeva nadesno, na visini svoje vrijednosti; prijelaz je strelica od razine do razine, a broj u oznaci
                mora biti razlika vrijednosti s točnim predznakom; profil reakcije: "vrhovi":[{"od":0,"do":1,"vrijednost":90,
                "oznaka":"bez katalizatora","isprekidano":false}] i energija aktivacije kao prijelaz {"od":0,"vrh":0,"oznaka":"Eₐ"};
                "\n" lomi oznaku u dva retka)
              strukturna_formula: {"tip":"strukturna_formula","atomi":[{"id":"C","simbol":"C","x":0,"y":0,"nevezni":0},
                {"id":"O1","simbol":"O","x":0,"y":1,"nevezni":2}],"veze":[["C","O1",2]],"zagrade":true,"naboj":"2−",
                "kutovi":[{"atomi":["O2","C","O3"],"oznaka":"120°"}]} (x, y u duljinama veze, y prema gore; veza 1, 2, 3, "klin",
                "crtkano" ili "vodikova" (između molekula); "nevezni" = broj neveznih parova, samo u Lewisovoj formuli i tada uz
                svaki atom, a provjerava se oktet i naboj; skupina poput "CH₃" ili "OH" smije biti jedan atom; asimetrični C-atom:
                "istaknut":true i "zvjezdica":true; "broj": broj C-atoma u lancu; "naboj" na atomu; prsten:
                "prsten":{"atomi":["C1",…],"krug":true} složi atome bez x, y u pravilan mnogokut, a atom s "uz":"C1" stavi izvan
                njega; "zrcalo":true dodaje zrcalnu sliku; "natpisi":[{"x":0,"y":-2,"tekst":"…"}])
              elektrokemijski_clanak: {"tip":"elektrokemijski_clanak","vrsta":"galvanski","posuda":"dvije_case",
                "lijevo":{"elektroda":"Zn","otopina":"ZnSO₄(aq)","uloga":"anoda","pol":"−"},"desno":{"elektroda":"Cu",
                "otopina":"CuSO₄(aq)","uloga":"katoda","pol":"+"},"most":"KNO₃(aq)","elektroni":"desno",
                "uredaj":{"vrsta":"V","oznaka":"1,10 V"}} (vrsta: galvanski ili elektroliza; posuda: dvije_case, u_cijev ili casa;
                elektroliza: "otopina" uz posudu, "uredaj":{"vrsta":"izvor"}, uz elektrodu "plin" i "boja" otopine: crvena,
                ružičasta, žuta, narančasta, zelena, plava ili bezbojna; "elektroni" izostavi kad učenik smjer tek treba odrediti)
              ljuske: {"tip":"ljuske","cestice":[{"simbol":"Na","protoni":11,"elektroni":[2,8,1]},{"simbol":"Na⁺","protoni":11,
                "elektroni":[2,8]}],"raspored":true} (do tri čestice; "polumjer" u pm crta veličine u pravom omjeru)
              cestice: {"tip":"cestice","posude":[{"oznaka":"prije","cestice":[{"vrsta":"H₂","broj":4},{"vrsta":"O₂","broj":3}]},
                {"oznaka":"poslije","cestice":[{"vrsta":"H₂O","broj":4},{"vrsta":"O₂","broj":1}]}],"strelica":true,"ocuvanje":true}
                (do tri posude, najviše 30 čestica u posudi; "ocuvanje" provjerava da je atoma jednako u svim posudama;
                "prikaz":"cestice" crta svaku česticu kao jednu kuglicu, npr. molekule plina u smjesi)
              kromatogram: {"tip":"kromatogram","podloga":"papir","fronta":6.0,"jedinica":"cm","mrlje":[{"udaljenost":4.5,
                "boja":"plava","oznaka":"plava"}]} (udaljenosti od startne linije; podloga papir ili kreda; "kote":false skriva
                udaljenosti kad ih učenik ne smije vidjeti)
              titracija: {"tip":"titracija","kiselina":{"formula":"HCl","c":0.1,"V":20,"Ka":null,"protoni":1},
                "luzina":{"formula":"NaOH","c":0.1},"indikator":"fenolftalein"} (krivulju pH aplikacija sama izračuna; slaba
                kiselina: Ka; dvije kiseline za usporedbu: "kiselina":[…]; indikator: fenolftalein, metiloranž ili bromtimolplavo;
                volumen lužine u točki ekvivalencije mora pisati u tekstu)
            """.stripTrailing();
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
            - Nikad ne crtaj graf ni sliku znakovima u tekstu.
            - Ako predložak ima skicu, napravi istu skicu (isti tip i ista građa) s novim brojevima, usklađenu s novim zadatkom
              i rješenjem; ako je nema, "skica": null.
            - Ako objašnjenje predloška ima skice (blokovi ```skica s JSON-om u jednom retku), napravi iste skice na istim
              mjestima, s novim brojevima koji se pojavljuju u novom objašnjenju.
            - Zadnji odjeljak „## ✅ Rezultati“ s po jednim retkom za svaki podzadatak je obavezan.

            Vrati json {"zadatak":"markdown teksta zadatka","objasnjenje":"markdown objašnjenja","rezultati":{"a":"konačan odgovor","b":"…"},
            "skica":opis skice ili null}.""";

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
        boolean chemistry = "kemija".equals(course.subject());
        boolean primary = "os78".equals(course.level());
        return SYSTEM.formatted(course.describe(), physics ? PHYSICS_NUMBERS : "", primary ? PRIMARY_MATH : "",
                primary ? LENGTH_PRIMARY : LENGTH_SECONDARY, String.join("; ", plan.assumedKnowledge(course)),
                material.isEmpty() ? FORMULA_WITHOUT_MATERIAL : FORMULA_FROM_MATERIAL, primary ? FORMULA_TEXTBOOK : FORMULA_DERIVATION,
                physics ? PHYSICS_MISTAKES : "", chemistry ? CHEMISTRY_SKETCHES : "", material);
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

    // Lekcija: naziv i stavke (ili tip, opis i primjer situacije za lekciju primjene), stavke izravnih preduvjeta i
    // stranice materijala na kojima se uči (iz opsega gradiva; lekcija primjene uzima stranice svojih preduvjeta).
    public String user(long courseId, long objectiveId) {
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
        List<String> pages = plan.outcomePages(courseId, jdbc.queryForList("""
                SELECT DISTINCT po.page_ids FROM knowledge_item k JOIN plan_outcome po ON po.outcome_id = k.outcome_id
                WHERE k.duplicate_of IS NULL AND (k.objective_id = :o OR (:application AND k.objective_id IN
                    (SELECT prerequisite_id FROM objective_prerequisite WHERE objective_id = :o)))""",
                o.addValue("application", PlanApplicationStep.LESSON_KIND.equals(l.get("kind"))), String.class));
        if (!pages.isEmpty()) sb.append("\n\nGRADIVO IZ MATERIJALA (odavde učenik uči za test):\n").append(String.join("\n\n", pages));
        return sb.toString();
    }
}
