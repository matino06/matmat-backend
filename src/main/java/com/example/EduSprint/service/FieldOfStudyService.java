package com.example.EduSprint.service;

import com.example.EduSprint.entity.FieldOfStudy;
import com.example.EduSprint.repository.FieldOfStudyRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Service
public class FieldOfStudyService {
    public final FieldOfStudyRepository fieldOfStudyRepository;

    public FieldOfStudyService(FieldOfStudyRepository fieldOfStudyRepository) {
        this.fieldOfStudyRepository = fieldOfStudyRepository;
    }

    public FieldOfStudy getFieldOfStudyById(Long fieldId) {
        Optional<FieldOfStudy> optionalFieldOfStudy = fieldOfStudyRepository.findById(fieldId);

        if (optionalFieldOfStudy.isPresent()) {
            return optionalFieldOfStudy.get();
        }
        return null;
    }
}
