package io.mopl.worker.notification;

import io.mopl.core.db.DbConstraintNames;
import io.mopl.core.event.dm.DirectMessageReceivedEvent;
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
public class DirectMessageNotificationListener {

  private final NotificationRepository notificationRepository;
  private final MessageSource messageSource;
  private final NotificationEventPublisher notificationEventPublisher;

  // DM 수신 이벤트를 수신해 알림을 저장한다.
  @KafkaListener(
      topics = KafkaTopics.DIRECT_MESSAGE_RECEIVED,
      properties =
          "spring.json.value.default.type=io.mopl.core.event.dm.DirectMessageReceivedEvent")
  public void handle(DirectMessageReceivedEvent event) {
    try {
      UUID eventIdUuid = parseUuid(event.eventId(), "eventId", event.eventId());
      UUID receiverIdUuid = parseUuid(event.receiverId(), "receiverId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.dm.received.title",
              new Object[] {event.senderName()},
              "새 메시지가 도착했습니다. 보낸 사람: " + event.senderName(),
              Locale.KOREAN);

      String content = event.content() != null ? event.content() : "";

      Notification notification =
          Notification.builder()
              .eventId(eventIdUuid)
              .receiverId(receiverIdUuid)
              .title(title)
              .content(content)
              .level(NotificationLevel.INFO)
              .build();
      Notification saved = notificationRepository.save(notification);
      notificationEventPublisher.publish(saved);
    } catch (DataIntegrityViolationException e) {
      handleDataIntegrityViolation(e, event.eventId());
    } catch (IllegalArgumentException e) {
      // UUID 파싱 오류는 parseUuid에서 로그 처리.
    }
  }

  private void handleDataIntegrityViolation(DataIntegrityViolationException e, String eventId) {
    Throwable cause = e.getMostSpecificCause();
    String message = cause != null ? cause.getMessage() : e.getMessage();

    if (message != null && message.contains(DbConstraintNames.UQ_NOTIFICATIONS_EVENT_ID)) {
      log.debug("중복 이벤트 무시 (eventId={})", eventId);
    } else if (message != null && message.contains(DbConstraintNames.FK_NOTIFICATIONS_RECEIVER)) {
      log.error("수신자 참조 오류 (eventId={})", eventId, e);
    } else {
      log.error("알림 저장 중 오류 (eventId={})", eventId, e);
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
