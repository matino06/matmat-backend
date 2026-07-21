package com.example.EduSprint.repository;

import com.example.EduSprint.dto.ObjectiveDTO;
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
                WITH RECURSIVE blocked AS (
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM account_objective ao_prereq
                        WHERE ao_prereq.account_id = :accountId
                        AND ao_prereq.objective_id = op.prerequisite_id
                        AND ao_prereq.n >= :tempo
                    )
                    UNION
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    JOIN blocked b ON op.prerequisite_id = b.objective_id
                )
                SELECT fo.field_name,
                       s.subfield_name,
                       lo.objective_id,
                       lo.objective_name,
                       NOT EXISTS (SELECT 1 FROM blocked b WHERE b.objective_id = lo.objective_id) AS is_unlocked,
                       COALESCE(ao_o.last_q > 3, false) AS is_sufficient
                FROM learning_objective lo
                INNER JOIN course_objective co ON lo.objective_id = co.objective_id AND co.course_id = :courseId
                LEFT JOIN subfield_of_study s ON lo.subfield_id = s.subfield_id
                NATURAL JOIN field_of_study fo
                LEFT JOIN account_objective ao_o ON ao_o.objective_id = lo.objective_id
                     AND ao_o.account_id = :accountId
                WHERE EXISTS (
                    SELECT 1 FROM task t WHERE t.objective_id = lo.objective_id
                )
                ORDER BY s.subfield_id, is_unlocked DESC,
                         (SELECT COUNT(*) FROM objective_prerequisite op WHERE op.objective_id = lo.objective_id),
                         lo.objective_id
            """, nativeQuery = true)
    List<Object[]> findObjectivesWithUnlockStatus(@Param("accountId") Long accountId, @Param("courseId") Long courseId, @Param("tempo") Short tempo);

    // Study map: same unlock logic as findObjectivesWithUnlockStatus, but returns field_id
    // and keeps a stable curriculum order (field -> subfield -> topological prerequisite depth) for grouping into blocks.
    // objective_depth computes the longest path from a prerequisite-free root, so within a subfield a prerequisite always precedes its dependents.
    @Query(value = """
                WITH RECURSIVE blocked AS (
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM account_objective ao_prereq
                        WHERE ao_prereq.account_id = :accountId
                        AND ao_prereq.objective_id = op.prerequisite_id
                        AND ao_prereq.n >= :tempo
                    )
                    UNION
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    JOIN blocked b ON op.prerequisite_id = b.objective_id
                ),
                objective_depth AS (
                    -- roots: objectives with no prerequisite -> depth 0
                    SELECT lo.objective_id, 0 AS depth
                    FROM learning_objective lo
                    WHERE NOT EXISTS (
                        SELECT 1 FROM objective_prerequisite op WHERE op.objective_id = lo.objective_id
                    )
                    UNION ALL
                    -- dependent depth = prerequisite depth + 1 (longest path)
                    SELECT op.objective_id, d.depth + 1
                    FROM objective_prerequisite op
                    JOIN objective_depth d ON op.prerequisite_id = d.objective_id
                )
                SELECT fo.field_id,
                       fo.field_name,
                       s.subfield_id,
                       s.subfield_name,
                       lo.objective_id,
                       lo.objective_name,
                       NOT EXISTS (SELECT 1 FROM blocked b WHERE b.objective_id = lo.objective_id) AS is_unlocked,
                       COALESCE(ao_o.last_q > 3, false) AS is_mastered,
                       ao_o.last_q AS last_q
                FROM learning_objective lo
                INNER JOIN course_objective co ON lo.objective_id = co.objective_id AND co.course_id = :courseId
                LEFT JOIN subfield_of_study s ON lo.subfield_id = s.subfield_id
                NATURAL JOIN field_of_study fo
                LEFT JOIN account_objective ao_o ON ao_o.objective_id = lo.objective_id
                     AND ao_o.account_id = :accountId
                WHERE EXISTS (
                    SELECT 1 FROM task t WHERE t.objective_id = lo.objective_id
                )
                ORDER BY fo.field_id, s.subfield_id,
                         COALESCE((SELECT MAX(depth) FROM objective_depth od WHERE od.objective_id = lo.objective_id), 0),
                         lo.objective_id
            """, nativeQuery = true)
    List<Object[]> findStudyMapObjectives(@Param("accountId") Long accountId, @Param("courseId") Long courseId, @Param("tempo") Short tempo);

    @Query(value = """
                WITH RECURSIVE blocked AS (
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM account_objective ao_prereq
                        WHERE ao_prereq.account_id = :accountId
                        AND ao_prereq.objective_id = op.prerequisite_id
                        AND ao_prereq.n >= :tempo
                    )
                    UNION
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    JOIN blocked b ON op.prerequisite_id = b.objective_id
                )
                SELECT lo.*
                FROM account_objective ao
                JOIN learning_objective lo ON ao.objective_id = lo.objective_id
                INNER JOIN course_objective co ON lo.objective_id = co.objective_id AND co.course_id = :courseId
                WHERE ao.account_id = :accountId
                AND EXISTS (SELECT 1 FROM task t WHERE t.objective_id = lo.objective_id)
                AND (ao.last_solved_date + (INTERVAL '1 day' * ao.i)) <= (CURRENT_DATE + INTERVAL '23 hours 59 minutes 59 seconds')
                AND NOT EXISTS (SELECT 1 FROM blocked b WHERE b.objective_id = lo.objective_id)
                ORDER BY ao.ef ASC, lo.subfield_id ASC, ao.objective_id ASC
                LIMIT 1
            """, nativeQuery = true)
    LearningObjective findNextLearningObjective(@Param("accountId") Long accountId, @Param("courseId") Long courseId, @Param("tempo") Short tempo);

    // Field-study variant of findNextLearningObjective: restricts selection to objectives inside :fieldId
    // and treats cross-field prerequisites as mastered. The prerequisite graph is restricted to edges
    // whose BOTH endpoints are in the field, so only in-field prerequisites gate unlocking.
    @Query(value = """
                WITH RECURSIVE field_objectives AS (
                    SELECT lo.objective_id
                    FROM learning_objective lo
                    JOIN subfield_of_study s ON lo.subfield_id = s.subfield_id
                    WHERE s.field_id = :fieldId
                ),
                blocked AS (
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    WHERE op.objective_id    IN (SELECT objective_id FROM field_objectives)
                      AND op.prerequisite_id IN (SELECT objective_id FROM field_objectives)
                      AND NOT EXISTS (
                          SELECT 1
                          FROM account_objective ao_prereq
                          WHERE ao_prereq.account_id = :accountId
                          AND ao_prereq.objective_id = op.prerequisite_id
                          AND ao_prereq.n >= :tempo
                      )
                    UNION
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    JOIN blocked b ON op.prerequisite_id = b.objective_id
                    WHERE op.objective_id IN (SELECT objective_id FROM field_objectives)
                )
                SELECT lo.*
                FROM account_objective ao
                JOIN learning_objective lo ON ao.objective_id = lo.objective_id
                INNER JOIN course_objective co ON lo.objective_id = co.objective_id AND co.course_id = :courseId
                JOIN subfield_of_study s ON lo.subfield_id = s.subfield_id
                WHERE ao.account_id = :accountId
                AND s.field_id = :fieldId
                AND EXISTS (SELECT 1 FROM task t WHERE t.objective_id = lo.objective_id)
                AND (ao.last_solved_date + (INTERVAL '1 day' * ao.i)) <= (CURRENT_DATE + INTERVAL '23 hours 59 minutes 59 seconds')
                AND NOT EXISTS (SELECT 1 FROM blocked b WHERE b.objective_id = lo.objective_id)
                ORDER BY ao.ef ASC, lo.subfield_id ASC, ao.objective_id ASC
                LIMIT 1
            """, nativeQuery = true)
    LearningObjective findNextLearningObjectiveInField(@Param("accountId") Long accountId, @Param("courseId") Long courseId, @Param("tempo") Short tempo, @Param("fieldId") Long fieldId);

    @Query(value = """
                WITH RECURSIVE blocked AS (
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM account_objective ao_prereq
                        WHERE ao_prereq.account_id = :accountId
                        AND ao_prereq.objective_id = op.prerequisite_id
                        AND ao_prereq.n >= :tempo
                    )
                    UNION
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    JOIN blocked b ON op.prerequisite_id = b.objective_id
                )
                SELECT lo.objective_name, (ao.last_solved_date + (INTERVAL '1 day' * ao.i))::DATE, ao.last_q
                FROM account_objective ao
                JOIN learning_objective lo ON ao.objective_id = lo.objective_id
                INNER JOIN course_objective co ON lo.objective_id = co.objective_id AND co.course_id = :courseId
                WHERE ao.account_id = :accountId
                AND EXISTS (SELECT 1 FROM task t WHERE t.objective_id = lo.objective_id)
                AND (ao.last_solved_date + (INTERVAL '1 day' * ao.i)) <= (CURRENT_DATE + INTERVAL '23 hours 59 minutes 59 seconds')
                AND NOT EXISTS (SELECT 1 FROM blocked b WHERE b.objective_id = lo.objective_id)
                ORDER BY ao.ef ASC, lo.subfield_id ASC, ao.objective_id ASC
            """, nativeQuery = true)
    List<Object[]> findObjectivesForToday(@Param("accountId") Long accountId, @Param("courseId") Long courseId, @Param("tempo") Short tempo);

    @Query(value = """
                WITH RECURSIVE blocked AS (
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM account_objective ao_prereq
                        WHERE ao_prereq.account_id = :accountId
                        AND ao_prereq.objective_id = op.prerequisite_id
                        AND ao_prereq.n >= :tempo
                    )
                    UNION
                    SELECT op.objective_id
                    FROM objective_prerequisite op
                    JOIN blocked b ON op.prerequisite_id = b.objective_id
                )
                SELECT lo.objective_name, (ao.last_solved_date + (INTERVAL '1 day' * ao.i))::DATE, ao.last_q
                FROM account_objective ao
                JOIN learning_objective lo ON ao.objective_id = lo.objective_id
                INNER JOIN course_objective co ON lo.objective_id = co.objective_id AND co.course_id = :courseId
                WHERE ao.account_id = :accountId
                AND EXISTS (SELECT 1 FROM task t WHERE t.objective_id = lo.objective_id)
                AND (ao.last_solved_date + (INTERVAL '1 day' * ao.i)) > (CURRENT_DATE + INTERVAL '23 hours 59 minutes 59 seconds')
                AND NOT EXISTS (SELECT 1 FROM blocked b WHERE b.objective_id = lo.objective_id)
                AND NOT EXISTS(
                    SELECT 1
                    FROM weak_objectives wo
                    WHERE wo.objective_id = lo.objective_id
                        AND wo.account_id = :accountId
                )
                ORDER BY (ao.last_solved_date + (INTERVAL '1 day' * ao.i))::DATE ASC, lo.subfield_id ASC, ao.objective_id ASC
            """, nativeQuery = true)
    List<Object[]> findScheduledObjectives(@Param("accountId") Long accountId, @Param("courseId") Long courseId, @Param("tempo") Short tempo);

    @Query(value = "SELECT COUNT(lo.*) FROM learning_objective lo " +
                   "INNER JOIN course_objective co ON lo.objective_id = co.objective_id AND co.course_id = :courseId " +
                   "WHERE lo.objective_id IN (SELECT wo.objective_id FROM weak_objectives wo WHERE wo.account_id = :accountId)",
            nativeQuery = true)
    short findWeakObjectivesByAccountAndCourse(@Param("accountId") Long accountId, @Param("courseId") Long courseId);

    @Query(value = """
                    SELECT lo.objective_name, ao.last_q
                    FROM learning_objective lo
                    INNER JOIN course_objective co 
                        ON lo.objective_id = co.objective_id 
                        AND co.course_id = :courseId
                    JOIN account_objective ao 
                        ON ao.objective_id = lo.objective_id
                            AND ao.account_id = :accountId
                    WHERE lo.objective_id IN (
                        SELECT wo.objective_id 
                        FROM weak_objectives wo 
                        WHERE wo.account_id = :accountId
                    )
                """, nativeQuery = true)
    List<Object[]> findWeakObjectivesWithLastQ(@Param("accountId") Long accountId, @Param("courseId") Long courseId);

    @Query(value = """
                    SELECT lo.*
                    FROM learning_objective lo
                    INNER JOIN course_objective co 
                        ON lo.objective_id = co.objective_id 
                        AND co.course_id = :courseId
                    JOIN account_objective ao 
                        ON ao.objective_id = lo.objective_id
                            AND ao.account_id = :accountId
                            AND ao.last_q != 0
                    WHERE lo.objective_id IN (
                        SELECT wo.objective_id 
                        FROM weak_objectives wo 
                        WHERE wo.account_id = :accountId
                    )
                    ORDER BY ao.last_q ASC, ao.last_solved_date ASC
                    LIMIT 1
                """, nativeQuery = true)
    LearningObjective findWeakObjectivesWithLastQNotZero(@Param("accountId") Long accountId, @Param("courseId") Long courseId);

    @Query(value = """
                    SELECT lo.*
                    FROM learning_objective lo
                    INNER JOIN course_objective co 
                        ON lo.objective_id = co.objective_id 
                        AND co.course_id = :courseId
                    JOIN account_objective ao 
                        ON ao.objective_id = lo.objective_id
                            AND ao.account_id = :accountId
                            AND ao.last_q = 0
                    WHERE lo.objective_id IN (
                        SELECT wo.objective_id 
                        FROM weak_objectives wo 
                        WHERE wo.account_id = :accountId
                    )
                    ORDER BY ao.last_solved_date ASC
                    LIMIT 1
                """, nativeQuery = true)
    LearningObjective findWeakObjectivesWithLastQEqualZero(@Param("accountId") Long accountId, @Param("courseId") Long courseId);

    // Admin dependency graph: all objectives belonging to a course, with field/subfield and task count.
    @Query(value = """
                SELECT lo.objective_id,
                       lo.objective_name,
                       s.subfield_id,
                       s.subfield_name,
                       f.field_id,
                       f.field_name,
                       (SELECT COUNT(*) FROM task t WHERE t.objective_id = lo.objective_id) AS task_count
                FROM learning_objective lo
                INNER JOIN course_objective co ON co.objective_id = lo.objective_id AND co.course_id = :courseId
                LEFT JOIN subfield_of_study s ON lo.subfield_id = s.subfield_id
                LEFT JOIN field_of_study f ON s.field_id = f.field_id
                ORDER BY f.field_id, s.subfield_id, lo.objective_id
            """, nativeQuery = true)
    List<Object[]> findCourseObjectiveNodes(@Param("courseId") Long courseId);

    // Admin dependency graph: prerequisite edges where both endpoints belong to the course.
    // A row (objective_id, prerequisite_id) means prerequisite_id must be learned before objective_id.
    @Query(value = """
                SELECT op.objective_id, op.prerequisite_id
                FROM objective_prerequisite op
                INNER JOIN course_objective co  ON co.objective_id  = op.objective_id    AND co.course_id  = :courseId
                INNER JOIN course_objective co2 ON co2.objective_id = op.prerequisite_id AND co2.course_id = :courseId
            """, nativeQuery = true)
    List<Object[]> findCoursePrerequisiteEdges(@Param("courseId") Long courseId);
}
