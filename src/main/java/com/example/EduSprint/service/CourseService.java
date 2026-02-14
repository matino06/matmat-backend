package com.example.EduSprint.service;

import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.CourseRepository;
import org.springframework.stereotype.Service;

@Service
public class CourseService {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public Course getCourseById(Long id) {
        return courseRepository.findCourseByCourseId(id).get();
    }
}
