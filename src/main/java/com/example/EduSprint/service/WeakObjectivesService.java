package com.example.EduSprint.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WeakObjectivesService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Briše sve zapise iz tablice weak_objectives svaki dan u 3:00 ujutro.
     */
    @Transactional
    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Zagreb")
    public void clearWeakObjectives() {
        String sql = "DELETE FROM weak_objectives";
        int deletedCount = entityManager.createNativeQuery(sql).executeUpdate();
        System.out.println("Očišćeno " + deletedCount + " zapisa iz weak_objectives.");
    }
}
