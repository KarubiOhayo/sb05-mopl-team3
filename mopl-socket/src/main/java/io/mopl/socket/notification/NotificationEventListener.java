package io.mopl.socket.notification;

import io.mopl.core.event.notification.NotificationCreatedEvent;
import io.mopl.core.event.notification.NotificationType;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.socket.dm.DirectMessageSubscriptionRegistry;
import io.mopl.socket.notification.dto.NotificationDto;
import io.mopl.socket.sse.SseService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final SseService sseService;
  private final DirectMessageSubscriptionRegistry dmSubscriptionRegistry;

  // 알림 생성 이벤트를 수신해 SSE로 전송한다.
  @KafkaListener(
      topics = KafkaTopics.NOTIFICATION_CREATED,
      groupId = "mopl-socket-notification-group",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.notification.NotificationCreatedEvent")
  @Transactional(readOnly = true)
  public void handleCreatedEvent(NotificationCreatedEvent event) {
    try {
      log.info("알림 생성 이벤트 수신: notificationId={}", event.notificationId());

      NotificationDto dto =
          NotificationDto.builder()
              .id(UUID.fromString(event.notificationId()))
              .createdAt(event.occurredAt())
              .receiverId(UUID.fromString(event.receiverId()))
              .title(event.title())
              .content(event.content())
              .level(event.level())
              .build();

      if (event.type() == NotificationType.DIRECT_MESSAGE
          && StringUtils.hasText(event.referenceId())
          && dmSubscriptionRegistry.isUserSubscribed(event.receiverId(), event.referenceId())) {
        log.info(
            "활성 대화는 알림 SSE를 건너뜁니다: receiverId={}, conversationId={}",
            event.receiverId(),
            event.referenceId());
        return;
      }

      sseService.send(event.receiverId(), "notifications", dto);
    } catch (Exception e) {
      log.error("알림 생성 이벤트 처리 중 오류 발생 (재시도/DLQ 예정)", e);
      throw e;
    }
  }
}
