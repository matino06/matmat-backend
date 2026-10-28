-- Which official courses the apps offer, and how they show them, now lives here rather than in the web's programs.js
-- and the iOS app's Program.swift: a course is offered once `published` is true (admins also see unpublished ones),
-- so a new subject goes live with an UPDATE instead of an App Store release. GET /account/offered-courses reads it.
-- Run before the backend deploy (ddl-auto=validate). Columns are additive; the data part fills today's values.
BEGIN;
ALTER TABLE course
    ADD COLUMN IF NOT EXISTS published    boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS position     integer,
    ADD COLUMN IF NOT EXISTS badge        varchar(8),
    ADD COLUMN IF NOT EXISTS hue          integer,
    ADD COLUMN IF NOT EXISTS short_name   varchar(60),
    ADD COLUMN IF NOT EXISTS matura_level varchar(5),
    ADD COLUMN IF NOT EXISTS level_hint   varchar(200),
    ADD COLUMN IF NOT EXISTS tagline      varchar(120),
    ADD COLUMN IF NOT EXISTS formula_pdf  varchar(200);

-- As programs.js had them; Ekonomska Matematika stays admin-only.
UPDATE course SET published = true, position = 1, badge = 'MA', hue = 239, short_name = 'Matematika', matura_level = 'A',
    level_hint = 'Zahtjevnija, za studije gdje matematika ima veću ulogu (STEM).', tagline = 'Državna matura',
    formula_pdf = '/pdfs/MAT-FORMULE.pdf' WHERE course_id = 1;
UPDATE course SET published = true, position = 2, badge = 'MB', hue = 215, short_name = 'Matematika', matura_level = 'B',
    level_hint = 'Osnovna, za ostale studije.', tagline = 'Državna matura',
    formula_pdf = '/pdfs/MAT-FORMULE.pdf' WHERE course_id = 2;
UPDATE course SET published = true, position = 3, badge = 'FIZ', hue = 280, short_name = 'Fizika', tagline = 'Državna matura',
    formula_pdf = '/pdfs/FIZ-FORMULE.pdf' WHERE course_id = 4;
UPDATE course SET course_name = btrim(course_name, E' \n\r'), published = false, position = 9, badge = 'EF', hue = 160, short_name = 'Ekonomska Matematika',
    tagline = 'Ekonomska Matematika na fakultetu EFZG', formula_pdf = '/pdfs/MAT-FORMULE.pdf' WHERE course_id = 3;

-- Kemija was admin 43's own course while it was built; now an official course, unpublished until it is ready:
-- UPDATE course SET published = true WHERE course_id = 13;
UPDATE course SET owner_account_id = NULL, status = NULL, learning_mode = NULL, level = NULL,
    published = false, position = 4, badge = 'KEM', hue = 25, short_name = 'Kemija', tagline = 'Državna matura',
    formula_pdf = '/pdfs/KEM-TABLICE.pdf' WHERE course_id = 13 AND subject = 'kemija';
COMMIT;
