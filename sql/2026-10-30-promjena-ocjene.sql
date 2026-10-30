-- An interactive lesson saves its suggested grade as soon as the student finishes, and the student may then change
-- it (PUT /solved-task/{id}/grade). To change a grade the backend needs the lesson's FSRS state from before that
-- rating, so each new solved_task keeps a copy of it (prev_*). Rows saved before this change have none (prev_n NULL)
-- and can't be changed.
-- weak_objectives follows the grade, so the trigger that fills it now also runs when the grade changes.
-- Idempotent and additive only: new nullable columns, no data changed. Run before deploying the backend that uses
-- them (JPA validates the schema).
BEGIN;
ALTER TABLE solved_task
    ADD COLUMN IF NOT EXISTS prev_i                smallint,
    ADD COLUMN IF NOT EXISTS prev_n                smallint,
    ADD COLUMN IF NOT EXISTS prev_stability        real,
    ADD COLUMN IF NOT EXISTS prev_difficulty       real,
    ADD COLUMN IF NOT EXISTS prev_last_grade       smallint,
    ADD COLUMN IF NOT EXISTS prev_last_solved_date timestamp;

DROP TRIGGER IF EXISTS trg_after_solved_task_insert ON solved_task;
CREATE TRIGGER trg_after_solved_task_insert
    AFTER INSERT OR UPDATE OF grade ON solved_task
    FOR EACH ROW EXECUTE FUNCTION after_solved_task_insert();
COMMIT;
