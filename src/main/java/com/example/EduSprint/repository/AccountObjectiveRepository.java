package com.example.EduSprint.repository;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.AccountObjective;
import com.example.EduSprint.entity.LearningObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountObjectiveRepository extends JpaRepository<AccountObjective, Long> {

    List<AccountObjective> findAllByAccount(Account account);

    @Query("SELECT ao FROM AccountObjective ao WHERE ao.account = :account AND ao.objective = :objective")
    Optional<AccountObjective> findByAccountAndObjective(@Param("account") Account account,
                                                         @Param("objective") LearningObjective objective);

}
