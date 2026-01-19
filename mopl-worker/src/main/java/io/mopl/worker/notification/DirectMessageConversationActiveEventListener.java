package io.mopl.worker.notification;

import io.mopl.core.event.dm.DirectMessageConversationActiveEvent;
import io.mopl.core.kafka.KafkaTopics;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageConversationActiveEventListener {

  private final DirectMessageActiveConversationTracker activeConversationTracker;

  // DM 대화 활성 상태를 수신해 알림 저장 여부 판단에 활용한다.
  @KafkaListener(
      topics = KafkaTopics.DIRECT_MESSAGE_CONVERSATION_ACTIVE,
      groupId = "mopl-worker-dm-active-group",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.dm.DirectMessageConversationActiveEvent")
  public void handle(DirectMessageConversationActiveEvent event) {
    try {
      UUID userId =
          NotificationListenerSupport.parseUuid(log, event.userId(), "userId", event.eventId());
      UUID conversationId =
          NotificationListenerSupport.parseUuid(
              log, event.conversationId(), "conversationId", event.eventId());

      activeConversationTracker.setActive(userId, conversationId, event.active());
    } catch (IllegalArgumentException e) {
      // UUID 파싱 오류는 parseUuid에서 로그 처리.
    }
  }
}
