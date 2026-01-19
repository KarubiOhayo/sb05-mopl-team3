package io.mopl.socket.dm;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DirectMessageSubscriptionRegistry {

  // 세션별 구독 대화를 기록하고, 유저별 대화 구독 여부를 계산한다.
  private final Map<String, Set<String>> sessionConversations = new ConcurrentHashMap<>();
  private final Map<String, String> sessionOwners = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Integer>> userConversationCounts =
      new ConcurrentHashMap<>();

  // 구독 등록: 세션과 유저, 대화의 연결을 기록한다.
  public void register(String sessionId, String userId, String conversationId) {
    if (sessionId == null || userId == null || conversationId == null) {
      return;
    }

    String existingUserId = sessionOwners.putIfAbsent(sessionId, userId);
    if (existingUserId != null && !existingUserId.equals(userId)) {
      log.warn(
          "세션 소유자 불일치: sessionId={}, existingUserId={}, userId={}",
          sessionId,
          existingUserId,
          userId);
    }

    Set<String> conversations =
        sessionConversations.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet());
    if (!conversations.add(conversationId)) {
      return;
    }

    userConversationCounts
        .computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
        .merge(conversationId, 1, Integer::sum);
  }

  public void unregister(String sessionId, String conversationId) {
    if (sessionId == null || conversationId == null) {
      return;
    }

    Set<String> conversations = sessionConversations.get(sessionId);
    if (conversations == null || !conversations.remove(conversationId)) {
      return;
    }

    String userId = sessionOwners.get(sessionId);
    if (userId == null) {
      return;
    }

    decrementCount(userId, conversationId);
  }

  // 세션 종료 시 해당 세션의 구독 대화를 모두 반환하고 정리한다.
  public RemovedSession removeSession(String sessionId) {
    if (sessionId == null) {
      return null;
    }

    String userId = sessionOwners.remove(sessionId);
    Set<String> conversations = sessionConversations.remove(sessionId);
    if (userId == null || conversations == null) {
      return null;
    }

    for (String conversationId : conversations) {
      decrementCount(userId, conversationId);
    }

    return new RemovedSession(userId, conversations);
  }

  // 해당 유저가 특정 대화 구독 중인지 확인한다.
  public boolean isUserSubscribed(String userId, String conversationId) {
    if (userId == null || conversationId == null) {
      return false;
    }

    Map<String, Integer> counts = userConversationCounts.get(userId);
    if (counts == null) {
      return false;
    }

    return counts.getOrDefault(conversationId, 0) > 0;
  }

  private void decrementCount(String userId, String conversationId) {
    userConversationCounts.computeIfPresent(
        userId,
        (key, counts) -> {
          Integer current = counts.get(conversationId);
          if (current == null) {
            return counts;
          }
          if (current <= 1) {
            counts.remove(conversationId);
          } else {
            counts.put(conversationId, current - 1);
          }
          return counts.isEmpty() ? null : counts;
        });
  }

  public record RemovedSession(String userId, Set<String> conversationIds) {}
}
