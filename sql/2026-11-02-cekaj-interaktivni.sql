-- Interaktivni vlastiti predmet: nova varijacija zadatka ne prikazuje se učeniku dok njezina interaktivna verzija nije
-- gotova (oko minute), jer većina želi interaktivni zadatak, a tekstualni koji stigne prvi zbunjuje. Ne uspije li
-- izrada interaktivne verzije, zadatak se odmah prikazuje kao tekst; zapne li (pauza, pad instance), prikazuje se
-- nakon 10 minuta.
-- task.lesson_ui_pending_since: kad je zadatak počeo čekati interaktivnu verziju; NULL = ne čeka.
-- shown_task: zadaci koje učenik smije dobiti; upiti koji biraju zadatak ili lekciju čitaju nju umjesto task.
-- Idempotentno i samo dodaje: novi stupac (NULL, pa su svi postojeći zadaci vidljivi) i pogled. Pokreni prije
-- deploya backenda koji ih koristi.
BEGIN;
ALTER TABLE task ADD COLUMN IF NOT EXISTS lesson_ui_pending_since timestamptz;

CREATE OR REPLACE VIEW shown_task AS
SELECT * FROM task
WHERE lesson_ui_pending_since IS NULL OR lesson_ui_pending_since < now() - interval '10 minutes';
COMMIT;
