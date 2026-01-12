package io.mopl.worker.notification;

import io.mopl.core.event.follow.UserFollowedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.notification.domain.Notification;
import io.mopl.worker.notification.domain.NotificationLevel;
import io.mopl.worker.notification.domain.NotificationRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowNotificationListener {

  private final NotificationRepository notificationRepository;
  private final MessageSource messageSource;

  @KafkaListener(
      topics = KafkaTopics.USER_FOLLOWED,
      properties = "spring.json.value.default.type=io.mopl.core.event.follow.UserFollowedEvent")
  public void handle(UserFollowedEvent event, Acknowledgment acknowledgment) {
    try {
      log.info("follow event received: eventId={}", event.eventId());

      UUID eventIdUuid = parseUuid(event.eventId(), "eventId", event.eventId());
      UUID followeeIdUuid = parseUuid(event.followeeId(), "followeeId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.follow.title", new Object[] {event.followerName()}, Locale.KOREAN);

      Notification notification =
          Notification.builder()
              .eventId(eventIdUuid)
              .receiverId(followeeIdUuid)
              .title(title)
              .content("")
              .level(NotificationLevel.INFO)
              .build();
      notificationRepository.save(notification);
    } catch (DataIntegrityViolationException e) {
      Throwable cause = e.getMostSpecificCause();
      String message = cause != null ? cause.getMessage() : e.getMessage();

      if (message != null && message.contains("uq_notifications_event_id")) {
        // 중복 이벤트 → 무시
        log.debug("중복 이벤트 무시 (eventId={})", event.eventId());
      } else if (message != null && message.contains("fk_notifications_receiver")) {
        log.error("수신자 외래키 위반 (eventId={})", event.eventId(), e);
      } else {
        log.error("데이터 무결성 오류 (eventId={})", event.eventId(), e);
      }
    } catch (IllegalArgumentException e) {
      // UUID 파싱 오류는 parseUuid에서 로깅됨
    } finally {
      acknowledgment.acknowledge();
    }
  }

  private UUID parseUuid(String value, String fieldName, String eventId) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      log.error("Invalid UUID format for {} (eventId={})", fieldName, eventId, e);
      throw e;
    }
  }
}
