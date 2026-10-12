-- Vlastiti predmet, ticket „Red poslova, trošak i mjesečna kvota“ (matmat-frontend #23).
-- Pokrenuti ručno na bazi PRIJE deploya backenda s ovom promjenom. Idempotentna.

BEGIN;

-- Posao izrade vlastitog predmeta: jedan korak (stranica, ishod, serija, lekcija, varijacija).
CREATE TABLE IF NOT EXISTS build_job (
    job_id          bigserial PRIMARY KEY,
    course_id       bigint      NOT NULL REFERENCES course (course_id),
    account_id      bigint      NOT NULL REFERENCES account (account_id),  -- kreator čija se kvota troši
    kind            varchar(40) NOT NULL,                                  -- vrsta koraka, npr. read_page
    ref             varchar(200),                                          -- što korak obrađuje, npr. stranica 12
    priority        smallint    NOT NULL DEFAULT 1,                        -- 0 = netko čeka, 1 = pozadina
    status          varchar(20) NOT NULL DEFAULT 'waiting',                -- waiting | running | done | failed | waiting_quota | paused | cancelled
    counts_quota    boolean     NOT NULL DEFAULT true,                     -- varijacije zadataka ne troše kvotu
    attempts        smallint    NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    lease_until     timestamptz,                                           -- posao u radu bez produženja se vraća u red
    input           text,
    output          text,
    error           text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    started_at      timestamptz,
    finished_at     timestamptz
);
CREATE INDEX IF NOT EXISTS build_job_pick_idx ON build_job (status, priority, next_attempt_at, job_id);
CREATE INDEX IF NOT EXISTS build_job_course_idx ON build_job (course_id);

-- Stvarni trošak svakog poziva OpenRoutera (usage.cost). Bez stranih ključeva:
-- ostaje i kad se predmet obriše, jer brisanje ne vraća potrošenu kvotu.
CREATE TABLE IF NOT EXISTS build_cost (
    cost_id      bigserial PRIMARY KEY,
    account_id   bigint        NOT NULL,
    course_id    bigint,
    job_id       bigint,
    model        varchar(100),
    cost_usd     numeric(10, 6) NOT NULL,
    counts_quota boolean       NOT NULL,
    created_at   timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS build_cost_account_month_idx ON build_cost (account_id, created_at);
CREATE INDEX IF NOT EXISTS build_cost_course_idx ON build_cost (course_id);

COMMIT;
