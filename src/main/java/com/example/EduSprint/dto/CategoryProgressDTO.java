package com.example.EduSprint.dto;

public class CategoryProgressDTO {
    private String categoryName;
    private Short totalObjectives;
    private Short masteredObjectives;

    public CategoryProgressDTO(String categoryName, Short totalObjectives, Short masteredObjectives) {
        this.categoryName = categoryName;
        this.totalObjectives = totalObjectives;
        this.masteredObjectives = masteredObjectives;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Short getTotalObjectives() {
        return totalObjectives;
    }

    public void setTotalObjectives(Short totalObjectives) {
        this.totalObjectives = totalObjectives;
    }

    public Short getMasteredObjectives() {
        return masteredObjectives;
    }

    public void setMasteredObjectives(Short masteredObjectives) {
        this.masteredObjectives = masteredObjectives;
    }

    @Override
    public String toString() {
        return "CategoryProgressDTO{" +
                "categoryName='" + categoryName + '\'' +
                ", totalObjectives=" + totalObjectives +
                ", masteredObjectives=" + masteredObjectives +
                '}';
    }
}
