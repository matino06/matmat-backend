package com.example.EduSprint.dto;

import java.util.List;

public class AdminCourseGraphDTO {
    private Long courseId;
    private String courseName;
    private List<AdminObjectiveNodeDTO> nodes;
    private List<AdminPrerequisiteEdgeDTO> edges;

    public AdminCourseGraphDTO(Long courseId, String courseName, List<AdminObjectiveNodeDTO> nodes, List<AdminPrerequisiteEdgeDTO> edges) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.nodes = nodes;
        this.edges = edges;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public List<AdminObjectiveNodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<AdminObjectiveNodeDTO> nodes) {
        this.nodes = nodes;
    }

    public List<AdminPrerequisiteEdgeDTO> getEdges() {
        return edges;
    }

    public void setEdges(List<AdminPrerequisiteEdgeDTO> edges) {
        this.edges = edges;
    }
}
