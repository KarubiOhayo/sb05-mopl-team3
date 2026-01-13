package io.mopl.api.conversation.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

  Optional<DirectMessage> findLatestByConversationId(UUID conversationId);
}
