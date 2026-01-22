package io.mopl.worker.conversation.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository
    extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

  @Query(
      "SELECT cp FROM ConversationParticipant cp WHERE cp.id.conversationId = :conversationId AND cp.id.userId != :senderId")
  Optional<ConversationParticipant> findReceiver(
      @Param("conversationId") UUID conversationId, @Param("senderId") UUID senderId);
}
