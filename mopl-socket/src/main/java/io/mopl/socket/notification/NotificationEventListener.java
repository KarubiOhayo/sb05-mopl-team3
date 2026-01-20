package io.mopl.socket.notification;

import io.mopl.core.event.notification.NotificationCreatedEvent;
import io.mopl.core.event.notification.NotificationType;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.redis.constants.RedisKeyPrefix;
import io.mopl.socket.dm.DirectMessageSubscriptionRegistry;
import io.mopl.socket.notification.dto.NotificationDto;
import io.mopl.socket.sse.SseService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private static final Duration UNREAD_COUNT_TTL = Duration.ofHours(1);

  private final SseService sseService;
  private final DirectMessageSubscriptionRegistry dmSubscriptionRegistry;
  private final RedisTemplate<String, String> redisTemplate;

  // 알림 생성 이벤트를 수신해 SSE로 전송한다.
  @KafkaListener(
      topics = KafkaTopics.NOTIFICATION_CREATED,
      groupId = "mopl-socket-notification-group",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.notification.NotificationCreatedEvent")
  public void handleCreatedEvent(NotificationCreatedEvent event) {
    try {
      incrementUnreadCount(event.receiverId());
      log.info("알림 생성 이벤트 수신: notificationId={}", event.notificationId());

      if (event.type() != null
          && event.type() == NotificationType.DIRECT_MESSAGE
          && StringUtils.hasText(event.referenceId())
          && dmSubscriptionRegistry.isUserSubscribed(event.receiverId(), event.referenceId())) {
        log.info(
            "활성 대화 중인 알림 SSE 건너뜀: receiverId={}, conversationId={}",
            event.receiverId(),
            event.referenceId());
        return;
      }

      UUID notificationId = parseUuid(event.notificationId(), "notificationId");
      UUID receiverId = parseUuid(event.receiverId(), "receiverId");
      if (notificationId == null || receiverId == null) {
        return;
      }

      NotificationDto dto =
          NotificationDto.builder()
              .id(notificationId)
              .createdAt(event.occurredAt())
              .receiverId(receiverId)
              .title(event.title())
              .content(event.content())
              .level(event.level())
              .build();

      sseService.send(receiverId.toString(), "notifications", dto);
    } catch (Exception e) {
      log.error("알림 생성 이벤트 처리 중 오류 발생 (재시도 또는 DLQ 예정)", e);
      throw e;
    }
  }

  private UUID parseUuid(String value, String fieldName) {
    try {
      return UUID.fromString(value);
    } catch (RuntimeException e) {
      log.warn("알림 이벤트 UUID 형식 오류로 무시: {}={}", fieldName, value);
      return null;
    }
  }

  private void incrementUnreadCount(String receiverId) {
    if (!StringUtils.hasText(receiverId)) {
      return;
    }
    try {
      String key = RedisKeyPrefix.NOTIFICATION_UNREAD_COUNT + receiverId;
      Long value = redisTemplate.opsForValue().increment(key);
      if (value != null && value == 1L) {
        redisTemplate.expire(key, UNREAD_COUNT_TTL);
      }
    } catch (Exception e) {
      log.warn("미읽음 알림 카운트 증가 실패: receiverId={}", receiverId, e);
    }
  }
}
