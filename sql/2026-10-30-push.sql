-- Push notifications for the iOS app (matino06/matmat-ios-app): the devices an account gets them on, and what it
-- wants. Sent pushes are recorded in email_log (types push_daily, push_streak, push_exam_<attempt id>).
-- Idempotent and additive only: two new tables, no data changed. Run before deploying the backend that uses them.
BEGIN;
CREATE TABLE IF NOT EXISTS push_device (
    token        varchar(200) PRIMARY KEY,
    account_id   bigint      NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    -- Debug builds from Xcode get tokens for APNs' sandbox; TestFlight and the App Store for production.
    sandbox      boolean     NOT NULL DEFAULT false,
    created_at   timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS push_device_account_idx ON push_device (account_id);

CREATE TABLE IF NOT EXISTS push_settings (
    account_id      bigint  PRIMARY KEY REFERENCES account (account_id) ON DELETE CASCADE,
    -- Daily reminder, minutes after midnight in Croatian time; NULL = no daily reminder.
    reminder_minute integer CHECK (reminder_minute BETWEEN 0 AND 1439),
    streak_warning  boolean NOT NULL DEFAULT true,
    exam_results    boolean NOT NULL DEFAULT true
);
COMMIT;
