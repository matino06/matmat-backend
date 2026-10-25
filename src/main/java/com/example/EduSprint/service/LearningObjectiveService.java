package com.example.EduSprint.service;

import com.example.EduSprint.dto.ObjectiveDTO;
import com.example.EduSprint.dto.ObjectiveWithStatusDTO;
import com.example.EduSprint.dto.StudyMapFieldDTO;
import com.example.EduSprint.dto.StudyMapObjectiveDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.repository.CourseRepository;
import com.example.EduSprint.repository.LearningObjectiveRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningObjectiveService {

    public final LearningObjectiveRepository learningObjectiveRepository;
    private final CourseRepository courseRepository;

    public LearningObjectiveService(LearningObjectiveRepository learningObjectiveRepository, CourseRepository courseRepository) {
        this.learningObjectiveRepository = learningObjectiveRepository;
        this.courseRepository = courseRepository;
    }

    public final List<LearningObjective> findAll() {
        return learningObjectiveRepository.findAll();
    }

    public final LearningObjective findById(Long id) {
        return learningObjectiveRepository.findById(id).orElse(null);
    }

    public final LearningObjective findNextLearningObjective(Long accountId, Long courseId, Short tempo) {
        LearningObjective learningObjective = learningObjectiveRepository.findNextLearningObjective(accountId, courseId, tempo);

        if (learningObjective == null) {
            return null;
        }

        return learningObjective;
    }

    public final LearningObjective findNextLearningObjectiveInField(Long accountId, Long courseId, Short tempo, Long fieldId) {
        return learningObjectiveRepository.findNextLearningObjectiveInField(accountId, courseId, tempo, fieldId);
    }

    public final List<ObjectiveDTO> findObjectivesForToday(Account account) {
        Long accountId = account.getAccountId();
        Long courseId = account.getCurrentCourse().getCourseId();
        Short tempo = account.getTempo();

        LocalDate today = LocalDate.now();
        List<Object[]> results = learningObjectiveRepository.findObjectivesForToday(accountId, courseId, tempo);
        List<ObjectiveDTO> objectiveDTOS = results.stream()
                .map(r -> new ObjectiveDTO(
                        (String) r[0],
                        ((java.sql.Date) r[1]).toLocalDate(),
                        (Short) r[2]
                ))
                .collect(Collectors.toList());

        Set<String> existingNames = objectiveDTOS.stream()
                .map(ObjectiveDTO::getTitle)
                .collect(Collectors.toSet());

        List<Object[]> weakObjectives = learningObjectiveRepository.findWeakObjectivesWithLastGrade(accountId, courseId);
        List<ObjectiveDTO> weakObjectiveDTOS = weakObjectives.stream()
                .map(r -> new ObjectiveDTO(
                        (String) r[0],
                        today,
                        (Short) r[1]
                ))
                .collect(Collectors.toList());

        weakObjectiveDTOS.stream()
                .filter(dto -> !existingNames.contains(dto.getTitle()))
                .forEach(objectiveDTOS::add);

        return objectiveDTOS;
    }

    public List<ObjectiveDTO> findScheduledObjectives(Account account) {
        Long accountId = account.getAccountId();
        Long courseId = account.getCurrentCourse().getCourseId();
        Short tempo = account.getTempo();

        List<Object[]> results = learningObjectiveRepository.findScheduledObjectives(accountId, courseId, tempo);
        List<ObjectiveDTO> objectiveDTOS = results.stream()
                .map(r -> new ObjectiveDTO(
                        (String) r[0],
                        ((java.sql.Date) r[1]).toLocalDate(),
                        (Short) r[2]
                ))
                .collect(Collectors.toList());

        return objectiveDTOS;
    }

    public List<StudyMapFieldDTO> getStudyMap(Account account) {
        Long accountId = account.getAccountId();
        Long courseId = account.getCurrentCourse().getCourseId();
        Short tempo = account.getTempo();

        List<Object[]> rows = learningObjectiveRepository.findStudyMapObjectives(accountId, courseId, tempo);

        List<StudyMapFieldDTO> fields = new ArrayList<>();
        StudyMapFieldDTO currentField = null;

        for (Object[] row : rows) {
            Long fieldId = ((Number) row[0]).longValue();
            String fieldName = (String) row[1];
            Long subfieldId = row[2] == null ? null : ((Number) row[2]).longValue();
            String subfieldName = (String) row[3];
            Long objectiveId = ((Number) row[4]).longValue();
            String objectiveName = (String) row[5];
            boolean unlocked = Boolean.TRUE.equals(row[6]);
            boolean mastered = Boolean.TRUE.equals(row[7]);
            Short lastGrade = row[8] == null ? null : ((Number) row[8]).shortValue();

            if (currentField == null || !currentField.getFieldId().equals(fieldId)) {
                currentField = new StudyMapFieldDTO(fieldId, fieldName, false, new ArrayList<>());
                fields.add(currentField);
            }

            currentField.getObjectives().add(
                    new StudyMapObjectiveDTO(objectiveId, objectiveName, subfieldId, subfieldName, unlocked, mastered, lastGrade));

            if (unlocked) {
                currentField.setUnlocked(true);
            }
        }

        return fields;
    }

    public LearningObjective getWeakObjectivesForAccountAndCurrentCourse(Account account) {
        Long accountId = account.getAccountId();
        Long courseId = account.getCurrentCourse().getCourseId();

        return learningObjectiveRepository.findWeakObjective(accountId, courseId);
    }

    public Short calculateExamProgress(Account account, LearningObjective objective, short lastGrade) {
        Long accountId = account.getAccountId();
        Long courseId = account.getCurrentCourse().getCourseId();
        Short tempo = account.getTempo();

        // Share of exam points per field, taken from the official exam catalogue.
        // A course without a catalogue (e.g. EFZG) has nothing to score against, so it reports 0.
        Map<String, Integer> distribution = courseRepository.findFieldWeightsByCourseId(courseId).stream()
                .collect(Collectors.toMap(arr -> (String) arr[0], arr -> ((Number) arr[1]).intValue()));
        if (distribution.isEmpty()) {
            return (short) 0;
        }

        List<ObjectiveWithStatusDTO> objectivesWithStatus = learningObjectiveRepository
                .findObjectivesWithUnlockStatus(accountId, courseId, tempo)
                .stream()
                .map(arr -> new ObjectiveWithStatusDTO(
                        (String) arr[0], (String) arr[1], (Long) arr[2],
                        (String) arr[3], (Boolean) arr[4], (Boolean) arr[5]))
                .toList();

        if (objective != null) {
            objectivesWithStatus.stream()
                    .filter(dto -> dto.getObjectiveId().equals(objective.getObjectiveId()))
                    .findFirst()
                    .ifPresent(dto -> dto.setIsMastered(lastGrade >= Fsrs.GOOD));
        }

        Map<String, FieldGroup> grouped = objectivesWithStatus.stream()
                .collect(Collectors.groupingBy(
                        ObjectiveWithStatusDTO::getFieldName,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    int total = list.size();
                                    long mastered = list.stream()
                                            .filter(obj -> Boolean.TRUE.equals(obj.getIsMastered()))
                                            .count();
                                    return new FieldGroup(total, mastered);
                                }
                        )
                ));

        double totalProgress = grouped.entrySet().stream()
                .mapToDouble(entry -> {
                    String fieldName = entry.getKey();
                    FieldGroup group = entry.getValue();
                    Integer points = distribution.get(fieldName);

                    if (points == null || group.total == 0) {
                        return 0.0;
                    }

                    return points * ((double) group.mastered / group.total);
                })
                .sum();

        return (short) Math.ceil(totalProgress);
    }

    public Short calculateExamProgress(Account account) {
        return calculateExamProgress(account, null, (short) 0);
    }

    private record FieldGroup(int total, long mastered) {}
}
