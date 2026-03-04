package com.example.EduSprint.service;

import com.example.EduSprint.entity.*;
import com.example.EduSprint.repository.AccountObjectiveRepository;
import com.example.EduSprint.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AccountObjectiveService {

    public final AccountObjectiveRepository accountObjectiveRepository;
    public final AccountRepository accountRepository;
    public final LearningObjectiveService learningObjectiveService;

    public AccountObjectiveService(AccountObjectiveRepository accountObjectiveRepository, AccountRepository accountRepository, LearningObjectiveService learningObjectiveService) {
        this.accountObjectiveRepository = accountObjectiveRepository;
        this.accountRepository = accountRepository;
        this.learningObjectiveService = learningObjectiveService;
    }

    @Transactional
    public boolean initializeAccountObjectives(Account account) {
        try {
            List<LearningObjective> objectives = learningObjectiveService.findAll();
            for (LearningObjective objective : objectives) {
                AccountObjective accountObjective = new AccountObjective();
                accountObjective.setAccount(account);
                accountObjective.setObjective(objective);
                accountObjective.setEf(2.5F);
                accountObjective.setI((short) 0);
                accountObjective.setN((short) 0);
                accountObjective.setLastSolvedDate(Instant.now());

                accountObjectiveRepository.save(accountObjective);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public boolean updateAccountObjective(SolvedTask newSolvedTask) {

        Account account = newSolvedTask.getAccount();
        Task task = newSolvedTask.getTask();
        LearningObjective objective = task.getObjective();

        Optional<AccountObjective> accountObjectiveOptional = accountObjectiveRepository.findByAccountAndObjective(account, objective);
        if (!accountObjectiveOptional.isPresent()) {
            return false;
        }

        AccountObjective accountObjective = accountObjectiveOptional.get();
        Float oldEF = accountObjective.getEf();
        Short oldI = accountObjective.getI();
        Short oldN = accountObjective.getN();
        Short q = newSolvedTask.getQ();
        Short lastQ = accountObjective.getLastQ();
        Instant newSolvedTaskEndTime = newSolvedTask.getEndTime();

        if (q >= 3) {
            Short newI;
            if (lastQ == null && q.equals((short)5)) {
                newI = 6;
            } else if (oldN == 0) {
                newI = 1;
            } else if (oldN == 1) {
                newI = 6;
            } else {
                newI = (short) Math.round(oldI * oldEF);
            }
            accountObjective.setI(newI);
            accountObjective.setN(++oldN);
        } else {
            accountObjective.setN((short) 0);
            accountObjective.setI((short) 1);
        }

        Float newEF = (float) (oldEF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)));
        newEF = (float) Math.max(newEF, 1.3);
        newEF = (float) Math.min(newEF, 2.5);

        accountObjective.setEf(newEF);
        accountObjective.setLastQ(q);
        accountObjective.setLastSolvedDate(newSolvedTaskEndTime);

        try {
            accountObjectiveRepository.save(accountObjective);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
