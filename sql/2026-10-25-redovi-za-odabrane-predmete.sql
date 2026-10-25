-- account_objective samo za predmete koje učenik uči (uz feat/redovi-za-odabrane-predmete).
--
-- Dosad je trigger after_learning_objective_insert svaku novu lekciju upisao svim računima, pa i lekcije tuđih
-- vlastitih predmeta. Sad red dobivaju samo računi kojima je predmet odabran (account.chosen_courses) ili trenutni.
-- Ostali ga dobiju kad odaberu predmet ili se prebace na njega (AccountObjectiveService.addCourseObjectives).
-- Postojeći redovi ostaju. Može ići prije ili poslije deploya backenda.

BEGIN;

DROP TRIGGER IF EXISTS after_learning_objective_insert ON learning_objective;
DROP FUNCTION IF EXISTS add_objective_to_all_accounts();

CREATE OR REPLACE FUNCTION add_course_objective_to_students() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    -- Vlastiti predmet: kreator dobiva redove pri potvrdi plana.
    INSERT INTO account_objective (account_id, objective_id, ef, i, n, last_solved_date)
    SELECT a.account_id, NEW.objective_id, 2.5, 0, 0, CURRENT_TIMESTAMP
    FROM account a
    JOIN course c ON c.course_id = NEW.course_id AND c.owner_account_id IS NULL
    WHERE NEW.course_id = ANY (a.chosen_courses) OR a.current_course_id = NEW.course_id
    ON CONFLICT (account_id, objective_id) DO NOTHING;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS after_course_objective_insert ON course_objective;
CREATE TRIGGER after_course_objective_insert AFTER INSERT ON course_objective
    FOR EACH ROW EXECUTE FUNCTION add_course_objective_to_students();

COMMIT;
