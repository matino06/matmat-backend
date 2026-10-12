-- Vlastiti predmet, ticket „Kreator i prazan vlastiti predmet“ (matmat-frontend #22).
-- Pokrenuti ručno na bazi PRIJE deploya backenda s ovom promjenom (ddl-auto=validate).
-- Skripta je idempotentna: može se pokrenuti više puta.

BEGIN;

-- Kreator i njegova mjesečna kvota (u dolarima troška OpenRoutera)
ALTER TABLE account ADD COLUMN IF NOT EXISTS is_creator boolean NOT NULL DEFAULT false;
ALTER TABLE account ADD COLUMN IF NOT EXISTS monthly_quota_usd numeric(8, 2) NOT NULL DEFAULT 10;

-- Vlastiti predmet je običan course s vlasnikom; null znači službeni predmet
ALTER TABLE course ADD COLUMN IF NOT EXISTS owner_account_id bigint REFERENCES account (account_id);
ALTER TABLE course ADD COLUMN IF NOT EXISTS status varchar(20);          -- nov | izrada | nacrt | potvrden
ALTER TABLE course ADD COLUMN IF NOT EXISTS learning_mode varchar(20);   -- ucenje_gradiva | brza_priprema
ALTER TABLE course ADD COLUMN IF NOT EXISTS level varchar(20);           -- os78 | ss
ALTER TABLE course ADD COLUMN IF NOT EXISTS exam_date date;
CREATE INDEX IF NOT EXISTS course_owner_account_id_idx ON course (owner_account_id);

-- Nazivi više nisu jedinstveni u cijeloj bazi: svaki kreator ima svoja područja, cjeline i lekcije.
DO $$
DECLARE c record;
BEGIN
  FOR c IN
    SELECT con.conname, rel.relname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE con.contype = 'u'
      AND array_length(con.conkey, 1) = 1
      AND ((rel.relname = 'field_of_study' AND att.attname = 'field_name')
        OR (rel.relname = 'subfield_of_study' AND att.attname = 'subfield_name')
        OR (rel.relname = 'learning_objective' AND att.attname = 'objective_name')
        OR (rel.relname = 'course' AND att.attname = 'course_name'))
  LOOP
    EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', c.relname, c.conname);
  END LOOP;
  -- jedinstvenost napravljena kao samostalan unique index (bez constrainta)
  FOR c IN
    SELECT idx.relname AS indexname
    FROM pg_index i
    JOIN pg_class idx ON idx.oid = i.indexrelid
    JOIN pg_class rel ON rel.oid = i.indrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = i.indkey[0]
    WHERE i.indisunique AND NOT i.indisprimary AND i.indnatts = 1
      AND NOT EXISTS (SELECT 1 FROM pg_constraint con WHERE con.conindid = i.indexrelid)
      AND ((rel.relname = 'field_of_study' AND att.attname = 'field_name')
        OR (rel.relname = 'subfield_of_study' AND att.attname = 'subfield_name')
        OR (rel.relname = 'learning_objective' AND att.attname = 'objective_name')
        OR (rel.relname = 'course' AND att.attname = 'course_name'))
  LOOP
    EXECUTE format('DROP INDEX %I', c.indexname);
  END LOOP;
END $$;

-- Naziv predmeta jedinstven je među službenima i unutar istog vlasnika
CREATE UNIQUE INDEX IF NOT EXISTS course_official_name_uq ON course (course_name) WHERE owner_account_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS course_owner_name_uq ON course (owner_account_id, course_name) WHERE owner_account_id IS NOT NULL;

COMMIT;

-- Kreatora se postavlja ručno, npr.:
-- UPDATE account SET is_creator = true WHERE email = '...';
