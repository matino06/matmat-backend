package com.example.EduSprint.controller;

import com.example.EduSprint.entity.FieldOfStudy;
import com.example.EduSprint.repository.FieldOfStudyRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/field-of-study")
public class FieldOfStudyController {
    public final FieldOfStudyRepository fieldOfStudyRepository;

    public FieldOfStudyController(FieldOfStudyRepository fieldOfStudyRepository) {
        this.fieldOfStudyRepository = fieldOfStudyRepository;
    }

    @GetMapping
    public List<FieldOfStudy> getAllFieldsOfStudy() {
        return fieldOfStudyRepository.findAll();
    }
}
