package com.example.EduSprint.repository;

import com.example.EduSprint.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findAccountByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query(value = "UPDATE account SET current_task_id = NULL WHERE current_task_id IS NOT NULL AND current_task_id != :defaultTaskId", nativeQuery = true)
    int resetCurrentTaskForAllExceptDefault(@Param("defaultTaskId") Long defaultTaskId);

    @Query("SELECT a FROM Account a WHERE a.learningRemindersEnabled = true " +
            "AND EXISTS (SELECT s FROM SolvedTask s WHERE s.account = a " +
            "           AND s.startTime BETWEEN :yesterdayStart AND :yesterdayEnd) " +
            "AND NOT EXISTS (SELECT s FROM SolvedTask s WHERE s.account = a " +
            "               AND s.startTime BETWEEN :todayStart AND :todayEnd)")
    List<Account> findAccountsToRemind(
            @Param("yesterdayStart") Instant yesterdayStart,
            @Param("yesterdayEnd") Instant yesterdayEnd,
            @Param("todayStart") Instant todayStart,
            @Param("todayEnd") Instant todayEnd);

    @Query("SELECT a FROM Account a WHERE a.learningRemindersEnabled = true")
    List<Account> findAllWithLearningRemindersEnabled();
}
