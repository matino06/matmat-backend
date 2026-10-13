-- Vlastiti predmet, 8. dio: skice u zadacima.
-- JSON opis skice uz zadatak (null = bez skice); aplikacija ga provjerava i iz njega crta SVG.
ALTER TABLE task ADD COLUMN IF NOT EXISTS sketch text;
