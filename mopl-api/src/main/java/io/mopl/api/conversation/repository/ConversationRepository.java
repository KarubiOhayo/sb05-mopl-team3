package io.mopl.api.conversation.repository;

import io.mopl.api.conversation.domain.Conversation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository
    extends JpaRepository<Conversation, UUID>, ConversationQueryRepository {}
