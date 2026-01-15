package io.mopl.api.conversation.repository;

import io.mopl.api.conversation.domain.DirectMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

  Optional<DirectMessage> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

  @Query(
      value =
          """
          select dm.*
          from direct_messages dm
          join (
            select conversation_id, max(created_at) as max_created_at
            from direct_messages
            where conversation_id in (:conversationIds)
            group by conversation_id
          ) latest on latest.conversation_id = dm.conversation_id
                and latest.max_created_at = dm.created_at
          where dm.conversation_id in (:conversationIds)
          """,
      nativeQuery = true)
  List<DirectMessage> findLatestByConversationIds(
      @Param("conversationIds") List<String> conversationIds);

  @Query(
      "SELECT dm FROM DirectMessage dm "
          + "WHERE dm.conversationId = :conversationId "
          + "ORDER BY dm.createdAt DESC, dm.id DESC")
  List<DirectMessage> findByConversationIdOrderByCreatedAtDescIdDesc(
      @Param("conversationId") UUID conversationId,
      org.springframework.data.domain.Pageable pageable);

  @Query(
      "SELECT dm FROM DirectMessage dm "
          + "WHERE dm.conversationId = :conversationId "
          + "AND dm.createdAt < :createdAt "
          + "ORDER BY dm.createdAt DESC, dm.id DESC")
  List<DirectMessage> findByConversationIdAndCursor(
      @Param("conversationId") UUID conversationId,
      @Param("createdAt") java.time.Instant createdAt,
      org.springframework.data.domain.Pageable pageable);

  @Query(
      "SELECT dm FROM DirectMessage dm "
          + "WHERE dm.conversationId = :conversationId "
          + "AND ("
          + "  dm.createdAt < :createdAt OR "
          + "  (dm.createdAt = :createdAt AND dm.id < :id)"
          + ") "
          + "ORDER BY dm.createdAt DESC, dm.id DESC")
  List<DirectMessage> findByConversationIdAndCursorWithId(
      @Param("conversationId") UUID conversationId,
      @Param("createdAt") java.time.Instant createdAt,
      @Param("id") UUID id,
      org.springframework.data.domain.Pageable pageable);
}
