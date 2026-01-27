package io.mopl.worker.conversation.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository
    extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

  @Query("SELECT cp FROM ConversationParticipant cp WHERE cp.id.conversationId = :conversationId")
  List<ConversationParticipant> findAllByConversationId(
      @Param("conversationId") UUID conversationId);
}
