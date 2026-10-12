package com.example.EduSprint.dto;

import java.util.List;

// Lekcija plana redom učenja, za pregled plana (samo čitanje dok ne dođe uređivanje).
public record PlanLessonDTO(long objectiveId, int position, String name, String concept, String kind,
                            String area, String unit, List<String> items, List<Long> prerequisites) {
}
