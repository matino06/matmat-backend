package com.example.EduSprint.service;

import com.example.EduSprint.dto.ExplanationStepDTO;
import com.example.EduSprint.entity.ExplanationStep;
import com.example.EduSprint.entity.Task;
import com.example.EduSprint.repository.ExplanationStepRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExplanationStepService {

    public final ExplanationStepRepository explanationStepRepository;

    public ExplanationStepService(ExplanationStepRepository explanationStepRepository) {
        this.explanationStepRepository = explanationStepRepository;
    }

    @Transactional
    public boolean saveSteps(String explanationSteps, Task task) throws JsonProcessingException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            List<ExplanationStepDTO> explanationStepsDTO = objectMapper
                    .readValue(explanationSteps, new TypeReference<List<ExplanationStepDTO>>() {
                    });

            for (ExplanationStepDTO explanationStepDTO : explanationStepsDTO) {
                ExplanationStep explanationStep = new ExplanationStep();
                explanationStep.setTask(task);
                explanationStep.setExplanation(explanationStepDTO.getExplanation());
                explanationStep.setStepNumber(explanationStepDTO.getStepNumber());
                explanationStep.setImageName(explanationStepDTO.getImageName());
                explanationStepRepository.save(explanationStep);
            }
            return true;
        } catch (Exception e) {
            System.out.println("Koraci" + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
