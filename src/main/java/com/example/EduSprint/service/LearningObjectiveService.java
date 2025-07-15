package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.repository.LearningObjectiveRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LearningObjectiveService {

    public final LearningObjectiveRepository learningObjectiveRepository;

    public LearningObjectiveService(LearningObjectiveRepository learningObjectiveRepository) {
        this.learningObjectiveRepository = learningObjectiveRepository;
    }

    public final List<LearningObjective> findAll() {
        return learningObjectiveRepository.findAll();
    }

    public final LearningObjective findById(Long id) {
        return learningObjectiveRepository.findById(id).orElse(null);
    }

    public final LearningObjective findNextLearningObjective(Long accountId) {

        LearningObjective learningObjective = learningObjectiveRepository.findNextLearningObjective(accountId);

        if (learningObjective == null) {
            return null;
        }

        return learningObjective;
    }
}
