package com.example.EduSprint.repository;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.AccountObjective;
import com.example.EduSprint.entity.LearningObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountObjectiveRepository extends JpaRepository<AccountObjective, Long> {

    List<AccountObjective> findAllByAccount(Account account);

    @Query("SELECT ao FROM AccountObjective ao WHERE ao.account = :account AND ao.objective = :objective")
    Optional<AccountObjective> findByAccountAndObjective(@Param("account") Account account,
                                                         @Param("objective") LearningObjective objective);

    // A new lesson (i = n = 0) for each lesson of the official courses :courseIds the account has no row for yet;
    // see AccountObjectiveService.addCourseObjectives. Flushes first, so a just-saved account is already in the table.
    @Modifying(flushAutomatically = true)
    @Query(value = """
                INSERT INTO account_objective (account_id, objective_id, ef, i, n, last_solved_date)
                SELECT DISTINCT :accountId, co.objective_id, 2.5, 0, 0, now()
                FROM course_objective co
                JOIN course c ON c.course_id = co.course_id AND c.owner_account_id IS NULL
                WHERE co.course_id IN (:courseIds)
                ON CONFLICT (account_id, objective_id) DO NOTHING
            """, nativeQuery = true)
    int addOfficialCourseObjectives(@Param("accountId") long accountId, @Param("courseIds") Collection<Long> courseIds);
}
