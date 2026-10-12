package com.example.EduSprint.controller;

import com.example.EduSprint.dto.DailyGoalDTO;
import com.example.EduSprint.dto.UserGoalDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.security.AuthPrincipal;
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

    // Goal, streak and calendar for the whole account; tasks from every course count.
    @GetMapping
    public ResponseEntity<UserGoalDTO> getUserGoalData(Authentication authentication) {
        AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();

        Account account = accountService.getAccount(authPrincipal.getEmail());
        Long accountId = account.getAccountId();
        int dailyGoal = account.getDailyGoal();

        List<DailyGoalDTO> calendar = solvedTaskService.getDailyGoalDTOs(accountId, dailyGoal);
        int[] streaks = solvedTaskService.calculateStreaks(calendar);

        LocalDate today = LocalDate.now();
        int todayCompleted = calendar.stream()
                .filter(day -> day.getDate().equals(today))
                .findFirst()
                .map(DailyGoalDTO::getCompleted)
                .orElse(0);

        UserGoalDTO userGoalDTO = new UserGoalDTO(
                dailyGoal,
                solvedTaskService.getTodayGoal(accountId, dailyGoal),
                todayCompleted,
                streaks[0],
                streaks[1],
                SolvedTaskService.STREAK_MIN_TASKS,
                account.getRegistrationDate(),
                calendar
        );
        return ResponseEntity.ok(userGoalDTO);
    }
}
