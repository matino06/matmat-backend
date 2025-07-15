package com.example.EduSprint.repository;

import com.example.EduSprint.entity.FieldOfStudy;
import com.example.EduSprint.entity.SubfieldOfStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubfieldOfStudyRepository extends JpaRepository<SubfieldOfStudy, Long> {
    List<SubfieldOfStudy> findAllByField(FieldOfStudy field);
}
