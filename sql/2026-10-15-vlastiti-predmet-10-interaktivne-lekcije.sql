-- Vlastiti predmet, 10. dio: interaktivne lekcije.
-- POKRENI PRIJE DEPLOYA backenda koji ima ova polja (ddl-auto=validate: bez stupaca se aplikacija ne pokrene).

-- Oblik lekcija koji kreator bira uz način učenja: tekst (zadatak i objašnjenje) ili interaktivno (uz to i lekcija od widgeta).
ALTER TABLE course ADD COLUMN IF NOT EXISTS lesson_format varchar(20) NOT NULL DEFAULT 'tekst';

-- Interaktivna verzija varijacije zadatka (JSON spec, verzija u polju "verzija"); null = samo tekst.
-- Tekst (task_text, explanation, results, sketch) ostaje: AI chat, ponavljanje i iOS rade s njim.
ALTER TABLE task ADD COLUMN IF NOT EXISTS lesson_ui text;
