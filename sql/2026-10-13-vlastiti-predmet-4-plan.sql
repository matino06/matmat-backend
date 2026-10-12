-- Vlastiti predmet, ticket „Izrada plana iz materijala“ (matmat-frontend #25).
-- Pokrenuti ručno na bazi PRIJE deploya backenda s ovom promjenom. Idempotentna.

BEGIN;

-- Isti korak se za isti predmet ne dodaje dvaput (npr. kad zadnja dva čitanja stranica završe istodobno,
-- oba bi htjela pokrenuti izradu plana).
CREATE UNIQUE INDEX IF NOT EXISTS build_job_step_uq ON build_job (course_id, kind, ref);

-- Ishodi (teme) s razradom, izvučeni iz kataloga ili skripte. Postaju cjeline plana.
CREATE TABLE IF NOT EXISTS plan_outcome (
    outcome_id bigserial PRIMARY KEY,
    course_id  bigint NOT NULL REFERENCES course (course_id),
    position   int    NOT NULL,
    code       varchar(100),
    title      text   NOT NULL,
    details    text                              -- razrada, JSON popis
);
CREATE INDEX IF NOT EXISTS plan_outcome_course_idx ON plan_outcome (course_id);

-- Zadaci sa starih ispita iz materijala: za stavke, pokrivenost i provjeru „preblizu“. Nikad se ne kopiraju.
CREATE TABLE IF NOT EXISTS exam_task (
    exam_task_id bigserial PRIMARY KEY,
    course_id    bigint NOT NULL REFERENCES course (course_id),
    material_id  bigint REFERENCES course_material (material_id) ON DELETE SET NULL,
    number       varchar(50),
    summary      text   NOT NULL,
    answer       text,
    needed       text                            -- potrebno znanje, JSON popis
);
CREATE INDEX IF NOT EXISTS exam_task_course_idx ON exam_task (course_id);

-- Stavke znanja: najsitnija stvar koju učenik mora znati. Lekcija ih ima 4–6.
CREATE TABLE IF NOT EXISTS knowledge_item (
    item_id       bigserial PRIMARY KEY,
    course_id     bigint      NOT NULL REFERENCES course (course_id),
    outcome_id    bigint      REFERENCES plan_outcome (outcome_id) ON DELETE SET NULL,
    objective_id  bigint,                         -- lekcija (learning_objective) kad je plan gotov
    position      int         NOT NULL,
    text          text        NOT NULL,
    kind          varchar(20),                    -- formula | definicija | pojava | postupak | zamka
    duplicate_of  bigint,                         -- uklonjena kao duplikat ove stavke
    prerequisites text                            -- izravni preduvjeti, JSON popis item_id
);
CREATE INDEX IF NOT EXISTS knowledge_item_course_idx ON knowledge_item (course_id);
CREATE INDEX IF NOT EXISTS knowledge_item_objective_idx ON knowledge_item (objective_id);

-- Lekcije kako ih je grupirao model, prije popravka kodom i upisa u learning_objective.
CREATE TABLE IF NOT EXISTS draft_lesson (
    draft_id   bigserial PRIMARY KEY,
    course_id  bigint NOT NULL REFERENCES course (course_id),
    outcome_id bigint NOT NULL REFERENCES plan_outcome (outcome_id) ON DELETE CASCADE,
    position   int    NOT NULL,
    name       text   NOT NULL,
    concept    text,
    items      text   NOT NULL                    -- JSON popis item_id
);
CREATE INDEX IF NOT EXISTS draft_lesson_course_idx ON draft_lesson (course_id);

-- Pretpostavljeno predznanje: ono što plan smije koristiti bez lekcije.
CREATE TABLE IF NOT EXISTS assumed_knowledge (
    assumed_id bigserial PRIMARY KEY,
    course_id  bigint NOT NULL REFERENCES course (course_id),
    text       text   NOT NULL
);
CREATE INDEX IF NOT EXISTS assumed_knowledge_course_idx ON assumed_knowledge (course_id);

-- Lekcija: jedna rečenica koncepta i vrsta (koncept | primjena); redoslijed učenja u predmetu.
ALTER TABLE learning_objective ADD COLUMN IF NOT EXISTS concept text;
ALTER TABLE learning_objective ADD COLUMN IF NOT EXISTS kind varchar(20) NOT NULL DEFAULT 'koncept';
ALTER TABLE course_objective ADD COLUMN IF NOT EXISTS position int;

COMMIT;
