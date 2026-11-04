-- Push to the Android app (matino06/matmat-android-app) through Firebase Cloud Messaging: each device says which
-- service it is on. Additive: existing rows are the iOS app's. FCM tokens are longer than APNs ones.
-- Idempotent. Run before deploying the backend that uses it.
BEGIN;
ALTER TABLE push_device ADD COLUMN IF NOT EXISTS platform varchar(10) NOT NULL DEFAULT 'ios';
ALTER TABLE push_device ALTER COLUMN token TYPE varchar(400);
COMMIT;
