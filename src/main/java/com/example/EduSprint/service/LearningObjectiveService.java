package com.example.EduSprint.service;

import com.example.EduSprint.dto.ObjectiveDTO;
import com.example.EduSprint.dto.ObjectiveWithStatusDTO;
import com.example.EduSprint.dto.StudyMapFieldDTO;
import com.example.EduSprint.dto.StudyMapObjectiveDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.repository.LearningObjectiveRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningObjectiveService {

    public final LearningObjectiveRepository learningObjectiveRepository;

    private static final Map<String, Integer> POINTS_DISTRIBUTION_COURSE_1 = Map.of(
            "Brojevi", 10,
            "Algebra i funkcije", 50,
            "Oblik i prostor", 15,
            "Mjerenje", 20,
            "Podatci, statistika i vjerojatnost", 5
    );

    private static final Map<String, Integer> POINTS_DISTRIBUTION_COURSE_2 = Map.of(
            "Brojevi", 20,
            "Algebra i funkcije", 40,
            "Oblik i prostor", 15,
            "Mjerenje", 15,
            "Podatci, statistika i vjerojatnost", 10
    );

    private static final Map<Long, Map<String, Integer>> COURSE_POINTS_DISTRIBUTION = Map.of(
            1L, POINTS_DISTRIBUTION_COURSE_1,
            2L, POINTS_DISTRIBUTION_COURSE_2
    );

    public LearningObjectiveService(LearningObjectiveRepository learningObjectiveRepository) {
        this.learningObjectiveRepository = learningObjectiveRepository;
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

        List<Object[]> weakObjectives = learningObjectiveRepository.findWeakObjectivesWithLastQ(accountId, courseId);
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
            Short lastQ = row[8] == null ? null : ((Number) row[8]).shortValue();

            if (currentField == null || !currentField.getFieldId().equals(fieldId)) {
                currentField = new StudyMapFieldDTO(fieldId, fieldName, false, new ArrayList<>());
                fields.add(currentField);
            }

            currentField.getObjectives().add(
                    new StudyMapObjectiveDTO(objectiveId, objectiveName, subfieldId, subfieldName, unlocked, mastered, lastQ));

            if (unlocked) {
                currentField.setUnlocked(true);
            }
        }

        return fields;
    }

    public LearningObjective getWeakObjectivesForAccountAndCurrentCourse(Account account) {
        Long accountId = account.getAccountId();
        Long courseId = account.getCurrentCourse().getCourseId();

        LearningObjective weakObjective = learningObjectiveRepository.findWeakObjectivesWithLastQNotZero(accountId, courseId);

        if (weakObjective != null) {
            return weakObjective;
        }

        return learningObjectiveRepository.findWeakObjectivesWithLastQEqualZero(accountId, courseId);
    }

    public Short calculateExamProgress(Account account, LearningObjective objective, short lastQ) {
        Long accountId = account.getAccountId();
        Long courseId = account.getCurrentCourse().getCourseId();
        Short tempo = account.getTempo();

        Map<String, Integer> distribution = COURSE_POINTS_DISTRIBUTION.get(courseId);
        if (distribution == null) {
            throw new IllegalArgumentException("No points distribution defined for course ID: " + courseId);
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
                    .ifPresent(dto -> dto.setIsMastered(lastQ >= 4));
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
