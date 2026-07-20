package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.entity.Task;
import com.example.EduSprint.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class TaskService {

    public final TaskRepository taskRepository;
    public final LearningObjectiveService learningObjectiveService;

    public TaskService(TaskRepository taskRepository, LearningObjectiveService learningObjectiveService) {
        this.taskRepository = taskRepository;
        this.learningObjectiveService = learningObjectiveService;
    }

    public Task getTaskById(Long taskId) {
        Optional<Task> optionalTask = taskRepository.findById(taskId);

        if (optionalTask.isPresent()) {
            return optionalTask.get();
        }
        return null;
    }

    public Task getNewTask(Account account) {
        Long accountId = account.getAccountId();
        Long courseId = account.getCurrentCourse().getCourseId();
        Short tempo = account.getTempo();

        LearningObjective nextLearningObjective = learningObjectiveService.findNextLearningObjective(accountId, courseId, tempo);

        LearningObjective weakObjective = learningObjectiveService.getWeakObjectivesForAccountAndCurrentCourse(account);
        if (nextLearningObjective == null && weakObjective == null) {
            return null;
        } else if (nextLearningObjective == null) {
            nextLearningObjective = weakObjective;
        }

        Long taskLimit = taskRepository.countByObjective(nextLearningObjective) - 1;
        return taskRepository.findTaskFromObjective(nextLearningObjective.getObjectiveId(), taskLimit, accountId);
    }

    public Task getTaskFromObjective(Account account, Long objectiveId) {
        LearningObjective objective = learningObjectiveService.findById(objectiveId);
        if (objective == null) {
            return null;
        }

        long taskCount = taskRepository.countByObjective(objective);
        if (taskCount == 0) {
            return null;
        }

        return taskRepository.findTaskFromObjective(objectiveId, taskCount - 1, account.getAccountId());
    }
}
