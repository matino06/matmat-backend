package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.entity.Task;
import com.example.EduSprint.repository.TaskRepository;
import org.springframework.stereotype.Service;

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
        LearningObjective nextLearningObjective;

        if (account.getCountWeakObjectives() > 0) {
            Random random = new Random();
            int index = random.nextInt(account.getWeakObjectives().size());
            nextLearningObjective = account.getWeakObjectives().get(index);

            return taskRepository.findRandomTaskFromObjective(nextLearningObjective.getObjectiveId());
        }

        nextLearningObjective = learningObjectiveService.findNextLearningObjective(account.getAccountId());

        if (nextLearningObjective == null && account.getWeakObjectives().isEmpty()) {
            return null;
        } else if (nextLearningObjective == null && !account.getWeakObjectives().isEmpty()) {
            Random random = new Random();
            int index = random.nextInt(account.getWeakObjectives().size());
            nextLearningObjective = account.getWeakObjectives().get(index);
        }
        return taskRepository.findRandomTaskFromObjective(nextLearningObjective.getObjectiveId());
    }
}
