-- Vlastiti predmet, ticket „Upload i čitanje materijala“ (matmat-frontend #24).
-- Pokrenuti ručno na bazi PRIJE deploya backenda s ovom promjenom. Idempotentna.

BEGIN;

-- Datoteka koju je kreator uploadao (original ostaje na S3).
CREATE TABLE IF NOT EXISTS course_material (
    material_id       bigserial PRIMARY KEY,
    course_id         bigint       NOT NULL REFERENCES course (course_id),
    s3_key            varchar(500) NOT NULL,
    original_name     varchar(300) NOT NULL,
    content_type      varchar(100) NOT NULL,
    size_bytes        bigint       NOT NULL,
    page_count        int          NOT NULL,
    available_on_exam boolean      NOT NULL DEFAULT false,  -- npr. knjižica formula: učenik je smije gledati
    created_at        timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS course_material_course_idx ON course_material (course_id);

-- Jedna stranica materijala: slika na S3, tekstni sloj PDF-a i pročitani Markdown s LaTeX-om.
CREATE TABLE IF NOT EXISTS material_page (
    page_id     bigserial PRIMARY KEY,
    material_id bigint       NOT NULL REFERENCES course_material (material_id) ON DELETE CASCADE,
    page_no     int          NOT NULL,
    image_key   varchar(500) NOT NULL,
    text_layer  text,
    markdown    text,
    kind        varchar(20),                -- kurikulum | ispit | rjesenja | skripta | ostalo
    UNIQUE (material_id, page_no)
);

COMMIT;
