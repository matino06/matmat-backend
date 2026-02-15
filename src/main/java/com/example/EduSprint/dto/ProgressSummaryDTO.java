package com.example.EduSprint.dto;

import java.util.List;

public class ProgressSummaryDTO {
    private List<ObjectiveDTO> todayObjectives;
    private List<ObjectiveDTO> futureObjectives;
    private List<CategoryProgressDTO> categories;

    public ProgressSummaryDTO(List<ObjectiveDTO> todayObjectives, List<ObjectiveDTO> futureObjectives, List<CategoryProgressDTO> categories) {
        this.todayObjectives = todayObjectives;
        this.futureObjectives = futureObjectives;
        this.categories = categories;
    }

    public ProgressSummaryDTO() {
    }

    public List<ObjectiveDTO> getTodayObjectives() {
        return todayObjectives;
    }

    public void setTodayObjectives(List<ObjectiveDTO> todayObjectives) {
        this.todayObjectives = todayObjectives;
    }

    public List<ObjectiveDTO> getFutureObjectives() {
        return futureObjectives;
    }

    public void setFutureObjectives(List<ObjectiveDTO> futureObjectives) {
        this.futureObjectives = futureObjectives;
    }

    public List<CategoryProgressDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryProgressDTO> categories) {
        this.categories = categories;
    }

    @Override
    public String toString() {
        return "ProgressSummaryDTO{" +
                "todayObjectives=" + todayObjectives +
                ", futureObjectives=" + futureObjectives +
                ", categories=" + categories +
                '}';
    }
}
