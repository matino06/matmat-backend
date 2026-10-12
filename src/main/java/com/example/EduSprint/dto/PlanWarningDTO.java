package com.example.EduSprint.dto;

// Upozorenje plana: nulta | redoslijed | velicina | rupa | ispit. Ozbiljna se na potvrdi priznaju kvačicom.
public record PlanWarningDTO(long warningId, String kind, Long objectiveId, String text, String detail,
                             boolean serious, boolean ignored) {
}
