package com.example.EduSprint.repository;

import com.example.EduSprint.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    List<AiMessage> findByConversation_ConversationIdAndStatusInOrderByCreatedAtAsc(Long conversationId, List<String> statuses);

    // Pitanja na koja AI nije odgovorio zbog greške (status 'error') ne ulaze u dnevni limit.
    @Query("SELECT m.createdAt FROM AiMessage m " +
            "WHERE m.conversation.account.accountId = :accountId " +
            "AND m.role = 'user' AND m.status <> 'error' AND m.createdAt > :since")
    List<Instant> findQuestionTimesSince(@Param("accountId") Long accountId, @Param("since") Instant since);

    Optional<AiMessage> findByMessageIdAndConversation_Account_AccountId(Long messageId, Long accountId);
}
