-- Vlastiti predmet, 7. dio: potvrda plana i prve varijacije zadataka.

-- Zadatak lekcije: koja je varijacija (1–4) i konačni rezultati po podzadatku (JSON {"a":"…","b":"…"}).
ALTER TABLE task ADD COLUMN IF NOT EXISTS variation int;
ALTER TABLE task ADD COLUMN IF NOT EXISTS results text;

-- Embedding zadatka s ispita (JSON popis brojeva), za provjeru je li novi zadatak preblizu zadatku s ispita.
ALTER TABLE exam_task ADD COLUMN IF NOT EXISTS embedding text;

-- Batch na OpenRouteru za prve varijacije velikog plana (više od 50 lekcija).
CREATE TABLE IF NOT EXISTS variation_batch (
    batch_id      varchar(100) PRIMARY KEY,
    course_id     bigint       NOT NULL REFERENCES course (course_id) ON DELETE CASCADE,
    account_id    bigint       NOT NULL,
    job_id        bigint,                          -- posao koji je poslao batch (trošak se upisuje uz njega)
    objective_ids text         NOT NULL,           -- JSON popis lekcija u batchu
    status        varchar(20)  NOT NULL DEFAULT 'u_tijeku',  -- u_tijeku | obrada | gotov | neuspio
    created_at    timestamptz  NOT NULL DEFAULT now(),
    finished_at   timestamptz
);
CREATE INDEX IF NOT EXISTS variation_batch_status_idx ON variation_batch (status);
