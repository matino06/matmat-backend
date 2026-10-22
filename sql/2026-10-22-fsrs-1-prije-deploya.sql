-- FSRS umjesto SM-2, 1. dio: pokreće se PRIJE deploya backenda. Stari backend i dalje radi (piše q, ne grade).
-- Ocjena je 1–4 (1 ponovno, 2 teško, 3 dobro, 4 lako). Stara ocjena q (0–5) ostaje u povijesti, a pretvara se ovako:
-- 0–2 → 1, 3 → 2, 4 → 3, 5 → 4. Stupci ef i last_q se više ne koriste, ali ostaju.
-- Idempotentno.
BEGIN;

ALTER TABLE solved_task ADD COLUMN IF NOT EXISTS grade smallint;
ALTER TABLE solved_task DROP CONSTRAINT IF EXISTS solved_task_grade_check;
ALTER TABLE solved_task ADD CONSTRAINT solved_task_grade_check CHECK (grade BETWEEN 1 AND 4);
ALTER TABLE solved_task ALTER COLUMN q DROP NOT NULL;
UPDATE solved_task
SET grade = CASE WHEN q <= 2 THEN 1 WHEN q = 3 THEN 2 WHEN q = 4 THEN 3 ELSE 4 END
WHERE grade IS NULL;

-- stabilnost i težina lekcije; prazne dok lekciju ne ocijeni FSRS (tada se izračunaju iz povijesti u solved_task)
ALTER TABLE account_objective ADD COLUMN IF NOT EXISTS stability real;
ALTER TABLE account_objective ADD COLUMN IF NOT EXISTS difficulty real;
ALTER TABLE account_objective ADD COLUMN IF NOT EXISTS last_grade smallint;
ALTER TABLE account_objective DROP CONSTRAINT IF EXISTS account_objective_last_grade_check;
ALTER TABLE account_objective ADD CONSTRAINT account_objective_last_grade_check CHECK (last_grade BETWEEN 1 AND 4);
UPDATE account_objective
SET last_grade = CASE WHEN last_q <= 2 THEN 1 WHEN last_q = 3 THEN 2 WHEN last_q = 4 THEN 3 ELSE 4 END
WHERE last_q IS NOT NULL AND stability IS NULL;

-- Slabe lekcije (ponavljaju se isti dan kad ništa drugo nije na redu): ocjena ponovno ili teško, kao prije q ≤ 3.
-- Dok radi stari backend, grade je prazan pa se ocjena uzima iz q.
CREATE OR REPLACE FUNCTION public.after_solved_task_insert()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE
    v_objective_id INTEGER;
    v_grade SMALLINT;
BEGIN
    SELECT t.objective_id
      INTO v_objective_id
      FROM task t
     WHERE t.task_id = NEW.task_id;

    v_grade := COALESCE(NEW.grade,
                        CASE WHEN NEW.q <= 2 THEN 1 WHEN NEW.q = 3 THEN 2 WHEN NEW.q = 4 THEN 3 ELSE 4 END);

    IF v_grade >= 3 THEN
        DELETE FROM weak_objectives
         WHERE account_id = NEW.account_id
           AND objective_id = v_objective_id;
    ELSE
        INSERT INTO weak_objectives (account_id, objective_id)
        VALUES (NEW.account_id, v_objective_id)
        ON CONFLICT DO NOTHING;
    END IF;

    RETURN NEW;
END;
$function$;

COMMIT;
