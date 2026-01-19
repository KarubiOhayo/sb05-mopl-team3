package io.mopl.worker.notification;

import io.mopl.core.db.DbConstraintNames;
import io.mopl.core.event.watching.WatchingSessionStartedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.notification.domain.Notification;
import io.mopl.worker.notification.domain.NotificationLevel;
import io.mopl.worker.notification.domain.NotificationRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
public class WatchingSessionNotificationListener {

  private final NotificationRepository notificationRepository;
  private final NotificationRecipientQuery recipientQuery;
  private final MessageSource messageSource;
  private final NotificationEventPublisher notificationEventPublisher;

  // 시청 시작 이벤트를 팔로워에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.WATCHING_SESSION_STARTED,
      properties =
          "spring.json.value.default.type=io.mopl.core.event.watching.WatchingSessionStartedEvent")
  public void handle(WatchingSessionStartedEvent event) {
    try {
      UUID watcherIdUuid = parseUuid(event.watcherId(), "watcherId", event.eventId());
      UUID contentIdUuid = parseUuid(event.contentId(), "contentId", event.eventId());

      String contentTitle = recipientQuery.findContentTitle(contentIdUuid);
      if (contentTitle == null || contentTitle.isBlank()) {
        contentTitle = "알 수 없는 콘텐츠";
      }

      String title =
          messageSource.getMessage(
              "notification.watching.started.title",
              new Object[] {event.watcherName(), contentTitle},
              event.watcherName() + "님이 " + contentTitle + " 시청을 시작했습니다.",
              Locale.KOREAN);

      List<UUID> receiverIds = recipientQuery.findFollowerIds(watcherIdUuid);
      for (UUID receiverId : receiverIds) {
        try {
          // 수신자별로 event_id를 분리해 중복 충돌을 방지한다.
          UUID eventIdUuid = toPerReceiverEventId(event.eventId(), receiverId);
          Notification notification =
              Notification.builder()
                  .eventId(eventIdUuid)
                  .receiverId(receiverId)
                  .title(title)
                  .content("")
                  .level(NotificationLevel.INFO)
                  .build();
          Notification saved = notificationRepository.save(notification);
          notificationEventPublisher.publish(saved);
        } catch (DataIntegrityViolationException ex) {
          handleDataIntegrityViolation(ex, event.eventId() + ":" + receiverId);
        }
      }
    } catch (IllegalArgumentException e) {
      // UUID 파싱 오류는 parseUuid에서 로그 처리.
    }
  }

  private UUID toPerReceiverEventId(String eventId, UUID receiverId) {
    String source = eventId + ":" + receiverId;
    return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
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
