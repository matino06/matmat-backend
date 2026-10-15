-- AI consent per account (iOS onboarding and Profil; the web follows later).
-- Idempotent and additive only: four nullable columns, no data changed.
BEGIN;
ALTER TABLE account ADD COLUMN IF NOT EXISTS ai_consent         boolean;
ALTER TABLE account ADD COLUMN IF NOT EXISTS ai_consent_version varchar(20);
ALTER TABLE account ADD COLUMN IF NOT EXISTS ai_consent_at      timestamptz;
ALTER TABLE account ADD COLUMN IF NOT EXISTS ai_consent_source  varchar(10);
COMMIT;
