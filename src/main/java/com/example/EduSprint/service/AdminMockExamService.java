package com.example.EduSprint.service;

import com.example.EduSprint.dto.AdminMockExamDetailDTO;
import com.example.EduSprint.dto.AdminMockExamQuestionDTO;
import com.example.EduSprint.dto.AdminMockExamQuestionUpdateDTO;
import com.example.EduSprint.dto.AdminMockExamSummaryDTO;
import com.example.EduSprint.dto.MockExamQuestionImageDTO;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.entity.MockExam;
import com.example.EduSprint.entity.MockExamQuestion;
import com.example.EduSprint.entity.MockExamQuestionImage;
import com.example.EduSprint.repository.MockExamQuestionRepository;
import com.example.EduSprint.repository.MockExamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Admin pregled i uređivanje probnih matura: vidi i neobjavljene mature i rješenja.
@Service
public class AdminMockExamService {

    private static final Set<String> OPTIONS = Set.of("A", "B", "C", "D");

    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;

    public AdminMockExamService(MockExamRepository mockExamRepository,
                                MockExamQuestionRepository mockExamQuestionRepository) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminMockExamSummaryDTO> listForCourse(Course course) {
        if (course == null) {
            return Collections.emptyList();
        }
        return mockExamRepository.findAllByCourseOrderByYearDescExamIdDesc(course).stream()
                .map(exam -> new AdminMockExamSummaryDTO(
                        exam.getExamId(),
                        exam.getTitle(),
                        exam.getSubtitle(),
                        exam.getYear(),
                        exam.getTerm(),
                        exam.getDurationMinutes(),
                        exam.getTotalPoints(),
                        exam.getIsPublished()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminMockExamDetailDTO getExam(Long examId) {
        MockExam exam = mockExamRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matura ne postoji"));

        MockExamService.QuestionTree tree = MockExamService.QuestionTree.of(
                mockExamQuestionRepository.findByExam_ExamIdOrderBySortOrderAsc(examId));

        List<AdminMockExamQuestionDTO> questions = tree.roots().stream()
                .map(root -> toQuestionDTO(root, tree.childrenByParent()))
                .toList();

        return new AdminMockExamDetailDTO(
                exam.getExamId(),
                exam.getTitle(),
                exam.getSubtitle(),
                exam.getYear(),
                exam.getTerm(),
                exam.getDurationMinutes(),
                exam.getTotalPoints(),
                exam.getIsPublished(),
                questions);
    }

    // Mijenja samo tekstualna polja; tip, bodovi, redoslijed, slike i kriteriji ostaju kakvi jesu.
    // Vraća pitanje bez podpitanja – frontend ih zadržava iz postojećeg stanja.
    @Transactional
    public AdminMockExamQuestionDTO updateQuestion(Long questionId, AdminMockExamQuestionUpdateDTO req) {
        if (req == null) {
            throw badRequest("Nedostaju podaci pitanja");
        }
        MockExamQuestion question = mockExamQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pitanje ne postoji"));

        String questionText = blankToNull(req.questionText());
        if (questionText == null) {
            throw badRequest("Tekst pitanja ne smije biti prazan");
        }
        String correctOption = blankToNull(req.correctOption());
        if (correctOption != null) {
            correctOption = correctOption.toUpperCase();
            if (!OPTIONS.contains(correctOption)) {
                throw badRequest("Točna opcija mora biti A, B, C ili D");
            }
        }

        question.setQuestionText(questionText);
        question.setOptionA(blankToNull(req.optionA()));
        question.setOptionB(blankToNull(req.optionB()));
        question.setOptionC(blankToNull(req.optionC()));
        question.setOptionD(blankToNull(req.optionD()));
        question.setCorrectOption(correctOption);
        question.setCorrectAnswer(blankToNull(req.correctAnswer()));
        question.setAnswerNotes(blankToNull(req.answerNotes()));
        question.setSolutionExplanation(blankToNull(req.solutionExplanation()));

        return toQuestionDTO(question, Map.of());
    }

    private AdminMockExamQuestionDTO toQuestionDTO(MockExamQuestion question,
                                                   Map<Long, List<MockExamQuestion>> childrenByParent) {
        List<AdminMockExamQuestionDTO> subDTOs = childrenByParent
                .getOrDefault(question.getQuestionId(), List.of()).stream()
                .sorted(Comparator.comparing(MockExamQuestion::getSortOrder))
                .map(c -> toQuestionDTO(c, childrenByParent))
                .toList();

        List<MockExamQuestionImageDTO> imageDTOs = question.getImages() == null
                ? List.of()
                : question.getImages().stream()
                        .sorted(Comparator.comparing(MockExamQuestionImage::getSortOrder))
                        .map(img -> new MockExamQuestionImageDTO(
                                img.getImageUrl(),
                                img.getAltText(),
                                img.getImageContext(),
                                img.getSortOrder()))
                        .toList();

        return new AdminMockExamQuestionDTO(
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
                question.getCorrectOption(),
                question.getCorrectAnswer(),
                question.getAnswerNotes(),
                question.getSolutionExplanation(),
                imageDTOs,
                subDTOs);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
