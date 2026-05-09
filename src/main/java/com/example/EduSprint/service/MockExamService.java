package com.example.EduSprint.service;

import com.example.EduSprint.dto.MockExamDetailDTO;
import com.example.EduSprint.dto.MockExamQuestionDTO;
import com.example.EduSprint.dto.MockExamQuestionImageDTO;
import com.example.EduSprint.dto.MockExamSummaryDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.entity.MockExam;
import com.example.EduSprint.entity.MockExamQuestion;
import com.example.EduSprint.entity.MockExamQuestionImage;
import com.example.EduSprint.repository.MockExamQuestionRepository;
import com.example.EduSprint.repository.MockExamRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MockExamService {

    private static final String IMAGE_CONTEXT_ANSWER = "answer";

    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;

    public MockExamService(MockExamRepository mockExamRepository,
                           MockExamQuestionRepository mockExamQuestionRepository) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
    }

    public List<MockExamSummaryDTO> getAvailableExamsForAccount(Account account) {
        Course course = account.getCurrentCourse();
        if (course == null) {
            return Collections.emptyList();
        }
        return mockExamRepository
                .findAllByCourseAndIsPublishedTrueOrderByYearDescExamIdDesc(course)
                .stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public MockExamDetailDTO getExamById(Long examId) {
        MockExam exam = mockExamRepository.findById(examId)
                .filter(e -> Boolean.TRUE.equals(e.getIsPublished()))
                .orElseThrow(() -> new EntityNotFoundException("Mock exam not found: " + examId));

        List<MockExamQuestion> all = mockExamQuestionRepository
                .findByExam_ExamIdOrderBySortOrderAsc(examId);

        Map<Long, List<MockExamQuestion>> childrenByParent = new HashMap<>();
        List<MockExamQuestion> roots = new ArrayList<>();
        for (MockExamQuestion q : all) {
            if (q.getParent() == null) {
                roots.add(q);
            } else {
                childrenByParent
                        .computeIfAbsent(q.getParent().getQuestionId(), k -> new ArrayList<>())
                        .add(q);
            }
        }

        List<MockExamQuestionDTO> questionDTOs = roots.stream()
                .map(root -> toQuestionDTO(root, childrenByParent))
                .toList();

        return new MockExamDetailDTO(
                exam.getExamId(),
                exam.getTitle(),
                exam.getSubtitle(),
                exam.getYear(),
                exam.getTerm(),
                exam.getDurationMinutes(),
                exam.getTotalPoints(),
                questionDTOs
        );
    }

    private MockExamSummaryDTO toSummaryDTO(MockExam exam) {
        return new MockExamSummaryDTO(
                exam.getExamId(),
                exam.getTitle(),
                exam.getSubtitle(),
                exam.getYear(),
                exam.getTerm(),
                exam.getDurationMinutes(),
                exam.getTotalPoints()
        );
    }

    private MockExamQuestionDTO toQuestionDTO(MockExamQuestion question,
                                              Map<Long, List<MockExamQuestion>> childrenByParent) {
        List<MockExamQuestion> children = childrenByParent.getOrDefault(question.getQuestionId(), List.of());
        List<MockExamQuestionDTO> subDTOs = children.stream()
                .sorted(Comparator.comparing(MockExamQuestion::getSortOrder))
                .map(c -> toQuestionDTO(c, childrenByParent))
                .toList();

        List<MockExamQuestionImageDTO> imageDTOs = question.getImages() == null
                ? List.of()
                : question.getImages().stream()
                        .filter(img -> !IMAGE_CONTEXT_ANSWER.equals(img.getImageContext()))
                        .sorted(Comparator.comparing(MockExamQuestionImage::getSortOrder))
                        .map(img -> new MockExamQuestionImageDTO(
                                img.getImageUrl(),
                                img.getAltText(),
                                img.getImageContext(),
                                img.getSortOrder()))
                        .toList();

        return new MockExamQuestionDTO(
                question.getQuestionId(),
                question.getQuestionNumber(),
                question.getQuestionType(),
                question.getQuestionText(),
                question.getPoints(),
                question.getSortOrder(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                imageDTOs,
                subDTOs
        );
    }
}
