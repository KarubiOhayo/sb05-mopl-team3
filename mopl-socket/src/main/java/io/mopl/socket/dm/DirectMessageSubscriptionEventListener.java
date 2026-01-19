package io.mopl.socket.dm;

import io.mopl.core.event.dm.DirectMessageConversationActiveEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.socket.websocket.security.SocketUserPrincipal;
import io.mopl.socket.websocket.session.SessionSubscriptionRegistry;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageSubscriptionEventListener {

  // DM 대화 구독 여부를 추적해 SSE 전송 여부 판단에 활용한다.
  private static final Pattern DM_DESTINATION_PATTERN =
      Pattern.compile("^/sub/conversations/([^/]+)/direct-messages$");

  private final SessionSubscriptionRegistry subscriptionRegistry;
  private final DirectMessageSubscriptionRegistry dmSubscriptionRegistry;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();
    String sessionId = accessor.getSessionId();
    String subscriptionId = accessor.getSubscriptionId();

    String conversationId = extractConversationId(destination);
    if (conversationId == null) {
      return;
    }

    if (StringUtils.hasText(sessionId)
        && StringUtils.hasText(subscriptionId)
        && StringUtils.hasText(destination)) {
      subscriptionRegistry.register(sessionId, subscriptionId, destination);
    }

    SocketUserPrincipal socketUser = resolvePrincipal(event.getUser());
    if (socketUser == null) {
      log.warn("사용자 인증 정보가 없어 DM 구독 이벤트를 무시했습니다. destination={}", destination);
      return;
    }

    dmSubscriptionRegistry.register(sessionId, socketUser.userId().toString(), conversationId);
    // 대화가 활성화되면 worker가 알림 저장을 건너뛰도록 이벤트를 발행한다.
    publishActiveEvent(socketUser.userId().toString(), conversationId, true);
  }

  @EventListener
  public void handleUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();
    String subscriptionId = accessor.getSubscriptionId();

    if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(subscriptionId)) {
      return;
    }

    String destination = subscriptionRegistry.unregister(sessionId, subscriptionId);
    String conversationId = extractConversationId(destination);
    if (conversationId == null) {
      return;
    }

    dmSubscriptionRegistry.unregister(sessionId, conversationId);
    // 대화가 비활성화되면 알림 저장이 다시 가능하도록 이벤트를 발행한다.
    String userId = resolveUserId(event.getUser());
    if (userId != null) {
      publishActiveEvent(userId, conversationId, false);
    }
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    DirectMessageSubscriptionRegistry.RemovedSession removed =
        dmSubscriptionRegistry.removeSession(event.getSessionId());
    if (removed == null) {
      return;
    }
    // 세션 종료 시 해당 대화들을 모두 비활성으로 전환한다.
    for (String conversationId : removed.conversationIds()) {
      publishActiveEvent(removed.userId(), conversationId, false);
    }
  }

  private String extractConversationId(String destination) {
    if (!StringUtils.hasText(destination)) {
      return null;
    }
    Matcher matcher = DM_DESTINATION_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return null;
    }
    return matcher.group(1);
  }

  private SocketUserPrincipal resolvePrincipal(Principal principal) {
    if (principal == null) {
      return null;
    }
    if (principal instanceof UsernamePasswordAuthenticationToken auth
        && auth.getPrincipal() instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }
    if (principal instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }
    return null;
  }

  private String resolveUserId(Principal principal) {
    SocketUserPrincipal socketUser = resolvePrincipal(principal);
    return socketUser == null ? null : socketUser.userId().toString();
  }

  private void publishActiveEvent(String userId, String conversationId, boolean active) {
    DirectMessageConversationActiveEvent event =
        new DirectMessageConversationActiveEvent(
            UUID.randomUUID().toString(), Instant.now(), userId, conversationId, active);
    kafkaTemplate.send(KafkaTopics.DIRECT_MESSAGE_CONVERSATION_ACTIVE, userId, event);
  }
}
