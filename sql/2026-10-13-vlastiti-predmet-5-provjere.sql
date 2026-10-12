-- Vlastiti predmet, ticket „Nulta pretpostavka i pokrivenost ispita pri izradi plana“ (matmat-frontend #26).
-- Pokrenuti ručno na bazi PRIJE deploya backenda s ovom promjenom. Idempotentna.

BEGIN;

-- Što je provjera nulte pretpostavke našla za lekciju (dva pokretanja, prijave se spajaju):
-- pojam koji lekcija koristi i stavke plana koje ga uče (ili „predznanje“ / „nigdje“).
CREATE TABLE IF NOT EXISTS zero_finding (
    finding_id   bigserial PRIMARY KEY,
    course_id    bigint      NOT NULL REFERENCES course (course_id),
    objective_id bigint      NOT NULL,
    run          smallint    NOT NULL,
    concept      text        NOT NULL,
    item_id      bigint,                          -- stavka ove lekcije koja koristi pojam
    taught_by    text        NOT NULL             -- JSON: id-ovi stavki, ili ["predznanje"] / ["nigdje"]
);
CREATE INDEX IF NOT EXISTS zero_finding_course_idx ON zero_finding (course_id);

-- Pokrivenost zadatka s ispita: koje ga lekcije pokrivaju i što nedostaje.
ALTER TABLE exam_task ADD COLUMN IF NOT EXISTS lessons text;   -- JSON popis objective_id
ALTER TABLE exam_task ADD COLUMN IF NOT EXISTS missing text;   -- prazno ako ništa ne nedostaje

-- Upozorenja plana koja kreator vidi pri pregledu (i može ih zanemariti).
CREATE TABLE IF NOT EXISTS plan_warning (
    warning_id      bigserial PRIMARY KEY,
    course_id       bigint      NOT NULL REFERENCES course (course_id),
    kind            varchar(20) NOT NULL,           -- nulta | redoslijed | velicina | rupa | ispit
    objective_id    bigint,
    prerequisite_id bigint,
    text            text        NOT NULL,
    detail          text,
    serious         boolean     NOT NULL DEFAULT false,  -- ozbiljno: kvačica na potvrdi
    ignored         boolean     NOT NULL DEFAULT false,
    created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS plan_warning_course_idx ON plan_warning (course_id);

COMMIT;
