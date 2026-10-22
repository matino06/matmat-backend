-- FSRS umjesto SM-2, 2. dio: pokreće se ODMAH NAKON deploya backenda (i webu i iOS-u s ocjenom 1–4).
-- Pretvara ocjene koje je stari backend spremio između 1. dijela i deploya, pa ocjena postaje obavezna.
-- Lekcije koje je FSRS već ocijenio (stability nije prazan) imaju točan last_grade i ne diraju se.
-- Idempotentno.
BEGIN;

UPDATE solved_task
SET grade = CASE WHEN q <= 2 THEN 1 WHEN q = 3 THEN 2 WHEN q = 4 THEN 3 ELSE 4 END
WHERE grade IS NULL;
ALTER TABLE solved_task ALTER COLUMN grade SET NOT NULL;

UPDATE account_objective
SET last_grade = CASE WHEN last_q <= 2 THEN 1 WHEN last_q = 3 THEN 2 WHEN last_q = 4 THEN 3 ELSE 4 END
WHERE last_q IS NOT NULL AND stability IS NULL;

COMMIT;
