package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountObjectiveService accountObjectiveService;
    private final TaskService taskService;
    private final CourseService courseService;

    public AccountService(AccountRepository accountRepository, AccountObjectiveService accountObjectiveService, TaskService taskService, CourseService courseService) {
        this.accountRepository = accountRepository;
        this.accountObjectiveService = accountObjectiveService;
        this.taskService = taskService;
        this.courseService = courseService;
    }

    @Transactional
    public boolean createAccount(String email, String name) {
        Course defaultCourse = courseService.getCourseById(1L);
        System.out.println(defaultCourse);
        Account account = new Account(email, name, defaultCourse);
        account.setCurrentTask(taskService.getTaskById(56L));
        try {
            accountRepository.save(account);
            accountObjectiveService.initializeAccountObjectives(account);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Account getAccount(String email) {
        Optional<Account> accountOptional = accountRepository.findAccountByEmail(email);
        if (accountOptional.isPresent()) {
            return accountOptional.get();
        } else {
            throw new RuntimeException("Account not found for email: " + email);
        }
    }

    @Transactional
    public boolean saveAccount(Account account) {
        try {
            accountRepository.save(account);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
