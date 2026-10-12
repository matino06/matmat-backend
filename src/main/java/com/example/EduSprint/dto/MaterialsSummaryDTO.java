package com.example.EduSprint.dto;

import java.util.List;

// Materijali predmeta s ograničenjima uploada i procjenom koliko će izrada potrošiti mjesečne kvote.
public record MaterialsSummaryDTO(List<MaterialDTO> materials, int totalPages, int maxPages, long totalBytes,
                                  long maxTotalBytes, long maxFileBytes, int estimatedQuotaPercent,
                                  int remainingQuotaPercent) {
}
