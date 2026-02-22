package com.example.EduSprint.service;

import com.example.EduSprint.entity.UserCourseGoal;
import com.example.EduSprint.repository.UserCourseGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCourseGoalService {
    public UserCourseGoalRepository userCourseGoalRepository;

    public UserCourseGoalService(UserCourseGoalRepository userCourseGoalRepository) {
        this.userCourseGoalRepository = userCourseGoalRepository;
    }

    @Transactional
    public boolean saveUserCourseGoal(UserCourseGoal userCourseGoal) {
        System.out.println(userCourseGoal);
        try {
            userCourseGoalRepository.save(userCourseGoal);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
