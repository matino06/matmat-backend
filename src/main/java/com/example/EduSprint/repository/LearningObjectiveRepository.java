package com.example.EduSprint.repository;

import com.example.EduSprint.entity.LearningObjective;
import com.example.EduSprint.entity.SubfieldOfStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningObjectiveRepository extends JpaRepository<LearningObjective, Long> {
    List<LearningObjective> findAllBySubfield(SubfieldOfStudy subfield);

    @Query("SELECT lo FROM LearningObjective lo " +
            "WHERE lo.subfield.subfieldId = :subfieldId " +
            "AND EXISTS (SELECT 1 FROM Task t WHERE t.objective = lo)")
    List<LearningObjective> findObjectivesBySubfieldWithTasks(@Param("subfieldId") Long subfieldId);

    @Query(value = """
                SELECT fo.field_name,
                       s.subfield_name,
                       lo.objective_id,
                       lo.objective_name,
                       (COUNT(op.prerequisite_id) = 0 OR
                        COUNT(op.prerequisite_id) = SUM(CASE WHEN ao_p.n >= 2 THEN 1 ELSE 0 END)) AS is_unlocked,
                       COALESCE(ao_o.last_q > 3, false) AS is_sufficient
                FROM learning_objective lo
                LEFT JOIN subfield_of_study s ON lo.subfield_id = s.subfield_id
                NATURAL JOIN field_of_study fo
                LEFT JOIN objective_prerequisite op ON op.objective_id = lo.objective_id
                LEFT JOIN account_objective ao_p ON ao_p.objective_id = op.prerequisite_id\s
                     AND ao_p.account_id = :accountId\s
                LEFT JOIN account_objective ao_o ON ao_o.objective_id = lo.objective_id\s
                     AND ao_o.account_id = :accountId\s
                WHERE EXISTS (
                    SELECT 1 FROM task t WHERE t.objective_id = lo.objective_id
                )
                GROUP BY s.subfield_id, fo.field_name, s.subfield_name, lo.objective_id, lo.objective_name, ao_o.last_q
                ORDER BY s.subfield_id, is_unlocked DESC, COUNT(op.prerequisite_id), lo.objective_id
            
            """, nativeQuery = true)
    List<Object[]> findObjectivesWithUnlockStatus(@Param("accountId") Long accountId);

    @Query(value = """
                SELECT lo.* 
                FROM account_objective ao
                JOIN learning_objective lo ON ao.objective_id = lo.objective_id
                WHERE ao.account_id = :accountId
                AND EXISTS (SELECT 1 FROM task t WHERE t.objective_id = lo.objective_id)
                AND (ao.last_solved_date + (INTERVAL '1 day' * ao.i)) <= (CURRENT_DATE + INTERVAL '23 hours 59 minutes 59 seconds')
                AND NOT EXISTS (
                    SELECT 1 
                    FROM objective_prerequisite op
                    WHERE op.objective_id = lo.objective_id
                    AND NOT EXISTS (
                        SELECT 1 
                        FROM account_objective ao_prereq
                        WHERE ao_prereq.account_id = :accountId
                        AND ao_prereq.objective_id = op.prerequisite_id
                        AND ao_prereq.n >= 2
                    )
                )
                ORDER BY ao.ef ASC, lo.subfield_id ASC, ao.objective_id ASC
                LIMIT 1
            """, nativeQuery = true)
    LearningObjective findNextLearningObjective(@Param("accountId") Long accountId);
}
