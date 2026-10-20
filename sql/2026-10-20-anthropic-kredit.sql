-- Potrošnja Anthropic API kredita (kredit uz Claude Max) za izradu vlastitih predmeta, AI chat i ocjenjivanje
-- mature. Ostatak kredita je CLAUDE_CREDIT_START minus zbroj ove tablice; bez nje sve ide na OpenRouter.
-- Bez account_id: brisanje računa ne smije vratiti potrošeni kredit.
-- Idempotentno i samo dodaje: nova tablica, a dosadašnja potrošnja Claude modela iz build_cost prepisuje se jednom.
BEGIN;
CREATE TABLE IF NOT EXISTS anthropic_cost (
    cost_id    bigserial PRIMARY KEY,
    source     varchar(20)    NOT NULL, -- own_course | chat | grading
    model      varchar(100)   NOT NULL,
    cost_usd   numeric(10, 6) NOT NULL,
    created_at timestamptz    NOT NULL DEFAULT now()
);
INSERT INTO anthropic_cost (source, model, cost_usd, created_at)
SELECT 'own_course', model, cost_usd, created_at FROM build_cost
WHERE model LIKE 'claude-%' AND NOT EXISTS (SELECT 1 FROM anthropic_cost);
COMMIT;
