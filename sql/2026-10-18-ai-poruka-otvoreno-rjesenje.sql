-- Uz pitanje u AI chatu sprema se je li učenik tada već otvorio rješenje zadatka,
-- da se iz razgovora vidi je li AI smio pokazati cijeli postupak.
-- Idempotentno i samo dodaje: jedan nullable stupac, postojeći podaci se ne mijenjaju.
BEGIN;
ALTER TABLE ai_message ADD COLUMN IF NOT EXISTS solution_revealed boolean;
COMMIT;
