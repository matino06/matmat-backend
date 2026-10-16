-- Reports of AI answers: a chat answer or the AI grading of a mock exam answer (matino06/matmat-ios-app#38).
-- Idempotent and additive only: one new table, no data changed.
BEGIN;
CREATE TABLE IF NOT EXISTS ai_report (
    report_id           bigserial PRIMARY KEY,
    account_id          bigint      NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    message_id          bigint      REFERENCES ai_message (message_id) ON DELETE CASCADE,
    mock_exam_answer_id bigint      REFERENCES mock_exam_answer (answer_id) ON DELETE CASCADE,
    reason              varchar(20) NOT NULL CHECK (reason IN ('incorrect', 'inappropriate')),
    note                text,
    source              varchar(10),
    created_at          timestamptz NOT NULL DEFAULT now(),
    CHECK ((message_id IS NULL) <> (mock_exam_answer_id IS NULL))
);
-- One report per student and answer; reporting again replaces the reason and note.
CREATE UNIQUE INDEX IF NOT EXISTS ai_report_message_uq ON ai_report (account_id, message_id) WHERE message_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ai_report_answer_uq ON ai_report (account_id, mock_exam_answer_id) WHERE mock_exam_answer_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ai_report_created_idx ON ai_report (created_at DESC);
COMMIT;
