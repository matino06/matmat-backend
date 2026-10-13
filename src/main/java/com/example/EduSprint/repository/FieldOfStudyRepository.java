package com.example.EduSprint.repository;

import com.example.EduSprint.entity.FieldOfStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FieldOfStudyRepository extends JpaRepository<FieldOfStudy, Long> {
    Optional<FieldOfStudy> findByFieldId(Integer fieldId);

    // Područja službenih predmeta: bez područja koja su nastala iz plana vlastitog predmeta.
    @org.springframework.data.jpa.repository.Query(value = """
                SELECT f.* FROM field_of_study f
                WHERE NOT EXISTS (SELECT 1 FROM course_field_weight w JOIN course c ON c.course_id = w.course_id
                                  WHERE w.field_id = f.field_id AND c.owner_account_id IS NOT NULL)
                  AND NOT EXISTS (SELECT 1 FROM subfield_of_study s JOIN learning_objective lo ON lo.subfield_id = s.subfield_id
                                  JOIN course_objective co ON co.objective_id = lo.objective_id
                                  JOIN course c ON c.course_id = co.course_id
                                  WHERE s.field_id = f.field_id AND c.owner_account_id IS NOT NULL)
                ORDER BY f.field_id
            """, nativeQuery = true)
    java.util.List<FieldOfStudy> findOfficial();
}
