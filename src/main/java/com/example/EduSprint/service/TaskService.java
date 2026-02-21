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

        List<LearningObjective> weakObjectives = learningObjectiveService.getWeakObjectivesForAccountAndCurrentCourse(account);
        if (nextLearningObjective == null && weakObjectives.isEmpty()) {
            return null;
        } else if (nextLearningObjective == null) {
            Random random = new Random();
            int index = random.nextInt(weakObjectives.size());
            nextLearningObjective = weakObjectives.get(index);
        }
        return taskRepository.findRandomTaskFromObjective(nextLearningObjective.getObjectiveId());
    }
}
