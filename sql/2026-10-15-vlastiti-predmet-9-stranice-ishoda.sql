-- Vlastiti predmet, 9. dio: stranice materijala uz ishod.
-- Opseg gradiva zapisuje na kojim se stranicama ishod obrađuje (JSON popis page_id), a stavke znanja izvlače
-- se samo iz tih stranica, da plan ne dodaje gradivo kojeg u materijalu nema.
ALTER TABLE plan_outcome ADD COLUMN IF NOT EXISTS page_ids text;
