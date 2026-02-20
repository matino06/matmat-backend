package com.example.EduSprint.controller;

import com.example.EduSprint.dto.DailyGoalDTO;
import com.example.EduSprint.dto.UserGoalDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.security.FirebasePrincipal;
import com.example.EduSprint.service.AccountService;
import com.example.EduSprint.service.SolvedTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/user-goal")
public class UserGoalController {

    public final SolvedTaskService solvedTaskService;
    public final AccountService accountService;

    public UserGoalController(SolvedTaskService solvedTaskService, AccountService accountService) {
        this.solvedTaskService = solvedTaskService;
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<UserGoalDTO> getUserGoalData(Authentication authentication) {
        FirebasePrincipal firebasePrincipal = (FirebasePrincipal) authentication.getPrincipal();

        Account account = accountService.getAccount(firebasePrincipal.getEmail());
        Long accountId = account.getAccountId();
        Course course = account.getCurrentCourse();

        LocalDate accountRegistrationDate = account.getRegistrationDate();
        List<DailyGoalDTO> calendar = solvedTaskService.getDailyGoalDTOs(accountId, course.getCourseId());

        int dailyGoal = account.getCourseGoals().stream()
                .filter(goal -> goal.getCourse().equals(course))
                .findFirst()
                .get()
                .getDailyGoal();

        UserGoalDTO userGoalDTO = new UserGoalDTO(dailyGoal, accountRegistrationDate, calendar);
        return ResponseEntity.ok(userGoalDTO);
    }
}
