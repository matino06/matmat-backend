package com.example.EduSprint.repository;

import com.example.EduSprint.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT t FROM Task t WHERE t.objective.objectiveId = :objectiveId")
    List<Task> findTasksByObjectiveId(@Param("objectiveId") Long objectiveId);

    @Query(value = """
                SELECT t.*
                FROM task t
                WHERE t.objective_id = :objectiveId
                ORDER BY RANDOM()
                LIMIT 1
            """, nativeQuery = true)
    Task findRandomTaskFromObjective(@Param("objectiveId") Long objectiveId);

    @Query(value = """
                SELECT t.*
                FROM task t
                WHERE t.objective_id = :objectiveId
                AND t.task_id != :taskId
                ORDER BY RANDOM()
                LIMIT 1
            """, nativeQuery = true)
    Task findRandomTaskFromObjective(@Param("objectiveId") Long objectiveId, @Param("taskId") Integer taskId);
}
