package io.mopl.api.conversation.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationParticipantRepository
    extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

  @Query(
      """
      select cp1.id.conversationId
      from ConversationParticipant cp1
      join ConversationParticipant cp2
        on cp1.id.conversationId = cp2.id.conversationId
      where cp1.id.userId = :userId and cp2.id.userId = :withUserId
      """)
  Optional<UUID> findConversationIdByParticipants(
      @Param("userId") UUID userId, @Param("withUserId") UUID withUserId);

  @Query("select cp from ConversationParticipant cp where cp.id.conversationId = :conversationId")
  List<ConversationParticipant> findAllByConversationId(
      @Param("conversationId") UUID conversationId);

  @Query(
      "select cp from ConversationParticipant cp "
          + "where cp.id.conversationId in :conversationIds and cp.id.userId = :userId")
  List<ConversationParticipant> findAllByConversationIdInAndUserId(
      @Param("conversationIds") List<UUID> conversationIds, @Param("userId") UUID userId);

  @Query(
      "select cp from ConversationParticipant cp "
          + "where cp.id.conversationId in :conversationIds and cp.id.userId <> :userId")
  List<ConversationParticipant> findAllByConversationIdInAndUserIdNot(
      @Param("conversationIds") List<UUID> conversationIds, @Param("userId") UUID userId);
}
