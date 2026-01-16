package io.mopl.worker.notification;

import io.mopl.core.event.user.UserRoleChangedEvent;
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
public class UserRoleNotificationListener {

  private final NotificationRepository notificationRepository;
  private final MessageSource messageSource;
  private final NotificationEventPublisher notificationEventPublisher;

  // 사용자 권한 변경 이벤트를 수신해 알림을 생성한다.
  @KafkaListener(
      topics = KafkaTopics.USER_ROLE_CHANGED,
      properties = "spring.json.value.default.type=io.mopl.core.event.user.UserRoleChangedEvent")
  public void handle(UserRoleChangedEvent event, Acknowledgment acknowledgment) {
    boolean shouldAck = false;
    try {
      UUID eventIdUuid = parseUuid(event.eventId(), "eventId", event.eventId());
      UUID userIdUuid = parseUuid(event.userId(), "userId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.user.role-changed.title",
              new Object[] {event.newRole()},
              Locale.KOREAN);

      Notification notification =
          Notification.builder()
              .eventId(eventIdUuid)
              .receiverId(userIdUuid)
              .title(title)
              .content("")
              .level(NotificationLevel.INFO)
              .build();
      Notification saved = notificationRepository.save(notification);
      notificationEventPublisher.publish(saved);
      shouldAck = true;
    } catch (DataIntegrityViolationException e) {
      handleDataIntegrityViolation(e, event.eventId());
      shouldAck = true;
    } catch (IllegalArgumentException e) {
      shouldAck = true;
      // UUID 형식 오류로 parseUuid가 실패한 경우.
    } finally {
      if (shouldAck) {
        acknowledgment.acknowledge();
      }
    }
  }

  private void handleDataIntegrityViolation(DataIntegrityViolationException e, String eventId) {
    Throwable cause = e.getMostSpecificCause();
    String message = cause != null ? cause.getMessage() : e.getMessage();

    if (message != null && message.contains("uq_notifications_event_id")) {
      log.debug("이미 처리된 이벤트입니다. 중복 저장을 건너뜁니다. (eventId={})", eventId);
    } else if (message != null && message.contains("fk_notifications_receiver")) {
      log.error("수신자 참조 무결성 오류가 발생했습니다. (eventId={})", eventId, e);
    } else {
      log.error("알림 저장 중 오류가 발생했습니다. (eventId={})", eventId, e);
    }
  }

  private UUID parseUuid(String value, String fieldName, String eventId) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      log.error("UUID 형식이 올바르지 않습니다. 필드: {} (eventId={})", fieldName, eventId, e);
      throw e;
    }
  }
}
