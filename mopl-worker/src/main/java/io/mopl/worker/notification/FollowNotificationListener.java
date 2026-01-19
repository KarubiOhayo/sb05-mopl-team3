package io.mopl.worker.notification;

import io.mopl.core.db.DbConstraintNames;
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
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowNotificationListener {

  private final NotificationRepository notificationRepository;
  private final MessageSource messageSource;
  private final NotificationEventPublisher notificationEventPublisher;

  @KafkaListener(
      topics = KafkaTopics.USER_FOLLOWED,
      properties = "spring.json.value.default.type=io.mopl.core.event.follow.UserFollowedEvent")
  public void handle(UserFollowedEvent event) {
    try {
      log.info("팔로우 이벤트 수신: eventId={}", event.eventId());

      UUID eventIdUuid = parseUuid(event.eventId(), "eventId", event.eventId());
      UUID followeeIdUuid = parseUuid(event.followeeId(), "followeeId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.follow.title",
              new Object[] {event.followerName()},
              "새 팔로우: " + event.followerName(),
              Locale.KOREAN);

      Notification notification =
          Notification.builder()
              .eventId(eventIdUuid)
              .receiverId(followeeIdUuid)
              .title(title)
              .content("")
              .level(NotificationLevel.INFO)
              .build();
      Notification saved = notificationRepository.save(notification);
      notificationEventPublisher.publish(saved);
    } catch (DataIntegrityViolationException e) {
      Throwable cause = e.getMostSpecificCause();
      String message = cause != null ? cause.getMessage() : e.getMessage();

      if (message != null && message.contains(DbConstraintNames.UQ_NOTIFICATIONS_EVENT_ID)) {
        // 이미 처리된 이벤트는 무시한다.
        log.debug("중복 이벤트 무시 (eventId={})", event.eventId());
      } else if (message != null && message.contains(DbConstraintNames.FK_NOTIFICATIONS_RECEIVER)) {
        log.error("수신자 참조 오류 (eventId={})", event.eventId(), e);
      } else {
        log.error("알림 저장 중 오류 (eventId={})", event.eventId(), e);
      }
    } catch (IllegalArgumentException e) {
      // UUID 파싱 오류는 parseUuid에서 로그 처리.
    }
  }

  private UUID parseUuid(String value, String fieldName, String eventId) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      log.error("UUID 형식이 올바르지 않습니다: {} (eventId={})", fieldName, eventId, e);
      throw e;
    }
  }
}
