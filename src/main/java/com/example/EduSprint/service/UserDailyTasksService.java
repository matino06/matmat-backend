package com.example.EduSprint.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Service
public class UserDailyTasksService {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Transactional(readOnly = true)
    public int getDailyTasksForToday(Long accountId, Long courseId) {
        String sql = """
            SELECT task_count
            FROM user_daily_tasks
            WHERE account_id = :accountId
              AND date = CURRENT_DATE
              AND course_id = :courseId
        """;
        Map<String, Object> params = Map.of(
                "accountId", accountId,
                "courseId", courseId
        );
        try {
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null ? count : -1;
        } catch (EmptyResultDataAccessException e) {
            return -1;
        }
    }
}
