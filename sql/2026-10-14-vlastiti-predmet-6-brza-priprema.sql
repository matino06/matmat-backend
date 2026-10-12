-- Vlastiti predmet, 6. dio: lekcije primjene za brzu pripremu.
-- Lekcija primjene (learning_objective.kind = 'primjena') nema stavke znanja; ima tip zadatka i primjer situacije
-- iz kojeg se poslije izrađuje zadatak.

ALTER TABLE learning_objective ADD COLUMN IF NOT EXISTS application_type varchar(20);  -- kombinacija | nova situacija | obrnuto | graf | zamka | omjer | miješana
ALTER TABLE learning_objective ADD COLUMN IF NOT EXISTS example_situation text;
