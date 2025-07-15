package com.example.EduSprint.service;

import com.example.EduSprint.entity.SubfieldOfStudy;
import com.example.EduSprint.repository.SubfieldOfStudyRepository;
import org.springframework.stereotype.Service;

@Service
public class SubfieldOfStudyService {

    public final SubfieldOfStudyRepository subfieldOfStudyRepository;

    public SubfieldOfStudyService(SubfieldOfStudyRepository subfieldOfStudyRepository) {
        this.subfieldOfStudyRepository = subfieldOfStudyRepository;
    }

    public SubfieldOfStudy findById(Long id) {
        return subfieldOfStudyRepository.findById(id).orElse(null);
    }
}
