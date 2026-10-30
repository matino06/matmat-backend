package com.example.EduSprint.dto;

/**
 * Spremljena ocjena: njezin id za promjenu ocjene (PUT /solved-task/{id}/grade) i za svaku ocjenu 1..4 dani do
 * sljedećeg ponavljanja (indeks ocjena - 1), računato od stanja lekcije prije ove ocjene, pa ih učenik vidi i kad
 * mijenja ocjenu. {@code days} je null ako račun nema stanje za tu lekciju.
 */
public record SavedRatingDTO(long solvedTaskId, short grade, short[] days) {
}
