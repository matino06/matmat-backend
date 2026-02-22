package com.example.EduSprint.repository;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.entity.UserCourseGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCourseGoalRepository extends JpaRepository<UserCourseGoal, Long> {
    Optional<UserCourseGoal> findUserCourseGoalByAccountAndCourse(Account account, Course course);
}
