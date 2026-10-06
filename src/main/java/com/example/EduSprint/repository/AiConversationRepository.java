package com.example.EduSprint.repository;

import com.example.EduSprint.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    Optional<AiConversation> findByConversationIdAndAccount_AccountId(Long conversationId, Long accountId);
}
