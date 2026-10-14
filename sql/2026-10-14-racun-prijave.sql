-- Račun s više prijava (iOS ticket „Račun s više prijava i ujednačene greške“, matmat-ios-app #24).
-- Pokrenuti ručno na bazi. Backend radi i prije nego što se skripta pokrene (tada račune traži samo po
-- emailu), ali prijave se počinju pamtiti tek kad tablica postoji. Skripta je idempotentna.

BEGIN;

-- Auth0 subject svake prijave (google-oauth2|…, apple|…) i račun kojem pripada. Jedan račun može imati
-- više prijava; postojeći računi dobivaju prijavu automatski pri sljedećoj prijavi (po emailu).
CREATE TABLE IF NOT EXISTS account_login (
    subject    varchar(255) PRIMARY KEY,
    account_id bigint       NOT NULL REFERENCES account (account_id) ON DELETE CASCADE,
    created_at timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS account_login_account_id_idx ON account_login (account_id);

COMMIT;
