package com.example.EduSprint.controller;

import com.example.EduSprint.entity.FieldOfStudy;
import com.example.EduSprint.entity.SubfieldOfStudy;
import com.example.EduSprint.repository.SubfieldOfStudyRepository;
import com.example.EduSprint.service.FieldOfStudyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subfield-of-study")
public class SubfieldOfStudyController {
    public final SubfieldOfStudyRepository subfieldOfStudyRepository;
    public final FieldOfStudyService fieldOfStudyService;

    public SubfieldOfStudyController(SubfieldOfStudyRepository subfieldOfStudyRepository, FieldOfStudyService fieldOfStudyService) {
        this.subfieldOfStudyRepository = subfieldOfStudyRepository;
        this.fieldOfStudyService = fieldOfStudyService;
    }

    @GetMapping("/{fieldId}")
    public List<SubfieldOfStudy> findByFieldId(@PathVariable Long fieldId) {
        FieldOfStudy field = fieldOfStudyService.getFieldOfStudyById(fieldId);
        return subfieldOfStudyRepository.findAllByField(field);
    }
}
