package io.mopl.worker.notification;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DirectMessageActiveConversationTracker {

  // 유저별로 활성 대화 ID를 보관한다.
  private final Map<UUID, Set<UUID>> activeConversations = new ConcurrentHashMap<>();

  // 활성/비활성 상태를 갱신한다.
  public void setActive(UUID userId, UUID conversationId, boolean active) {
    if (userId == null || conversationId == null) {
      return;
    }

    if (active) {
      activeConversations
          .computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet())
          .add(conversationId);
      return;
    }

    activeConversations.computeIfPresent(
        userId,
        (key, conversations) -> {
          conversations.remove(conversationId);
          return conversations.isEmpty() ? null : conversations;
        });
  }

  public boolean isActive(UUID userId, UUID conversationId) {
    if (userId == null || conversationId == null) {
      return false;
    }
    Set<UUID> conversations = activeConversations.get(userId);
    return conversations != null && conversations.contains(conversationId);
  }
}
