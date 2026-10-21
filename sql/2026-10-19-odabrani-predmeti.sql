-- Subjects a student chose to study (onboarding, then "Uredi predmete" on the web): ids of official courses, the
-- course switcher shows only these. NULL until the student chooses; until then the current course stands in.
-- Idempotent and additive only: one nullable column, no data changed.
BEGIN;
ALTER TABLE account ADD COLUMN IF NOT EXISTS chosen_courses bigint[];
COMMIT;
