package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.AccountRepository;
import com.example.EduSprint.security.AccountResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountObjectiveService accountObjectiveService;
    private final TaskService taskService;
    private final CourseService courseService;
    private final AccountResolver accountResolver;

    public AccountService(AccountRepository accountRepository, AccountObjectiveService accountObjectiveService, TaskService taskService, CourseService courseService, AccountResolver accountResolver) {
        this.accountResolver = accountResolver;
        this.accountRepository = accountRepository;
        this.accountObjectiveService = accountObjectiveService;
        this.taskService = taskService;
        this.courseService = courseService;
    }

    @Transactional
    public boolean createAccount(String email, String name, String loginSubject) {
        Course defaultCourse = courseService.getCourseById(1L);
        System.out.println(defaultCourse);
        Account account = new Account(email, name, defaultCourse);
        account.setCurrentTask(taskService.getTaskById(56L));
        try {
            accountRepository.save(account);
            accountObjectiveService.initializeAccountObjectives(account);
            accountResolver.linkNewAccount(loginSubject, account.getAccountId(), email);
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
            throw new AccountNotFoundException();
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

    public void updateNotificationSettings(String email, boolean learningReminders, boolean featureAnnouncements) {
        Account account = getAccount(email);
        if (account != null) {
            account.setLearningRemindersEnabled(learningReminders);
            account.setFeatureAnnouncementsEnabled(featureAnnouncements);
            accountRepository.save(account);
        }
    }

    public List<Account> getAllAccountsWithRemindersEnabled() {
        return accountRepository.findAllWithLearningRemindersEnabled();
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Zagreb")
    public void resetNonDefaultTasks() {
        int updatedCount = accountRepository.resetCurrentTaskForAllExceptDefault(56L);
        System.out.println("Resetiranje zadataka: " + updatedCount + " korisnika ažurirano.");
    }
}
