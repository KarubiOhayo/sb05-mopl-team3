package io.mopl.api.conversation.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationParticipantRepository
    extends JpaRepository<ConversationParticipant, ConversationParticipantId> {}
