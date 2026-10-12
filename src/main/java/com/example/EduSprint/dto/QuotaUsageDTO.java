package com.example.EduSprint.dto;

import java.time.LocalDate;

// Mjesečna kvota kako je vidi kreator: samo postotak, bez dolara, i dan obnove.
public record QuotaUsageDTO(int usedPercent, LocalDate resetsOn) {
}
