package com.PromptIQ.backend.chat.repository;
import com.PromptIQ.backend.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    Optional<Conversation> findByIdAndDeletedAtIsNull(UUID id);
}