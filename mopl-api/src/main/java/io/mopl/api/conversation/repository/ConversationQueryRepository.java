package io.mopl.api.conversation.repository;

import io.mopl.api.conversation.dto.ConversationPage;
import io.mopl.api.conversation.dto.ConversationSearchRequest;
import java.util.UUID;

public interface ConversationQueryRepository {
  ConversationPage findConversationPage(UUID userId, ConversationSearchRequest request);

  long countConversations(UUID userId, String keywordLike);
}
