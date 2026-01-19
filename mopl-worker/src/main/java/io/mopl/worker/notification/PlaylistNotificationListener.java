package io.mopl.worker.notification;

import io.mopl.core.db.DbConstraintNames;
import io.mopl.core.event.playlist.PlaylistContentAddedEvent;
import io.mopl.core.event.playlist.PlaylistCreatedEvent;
import io.mopl.core.event.playlist.PlaylistSubscribedEvent;
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
public class PlaylistNotificationListener {

  private final NotificationRepository notificationRepository;
  private final NotificationRecipientQuery recipientQuery;
  private final MessageSource messageSource;
  private final NotificationEventPublisher notificationEventPublisher;

  // 플레이리스트 구독 이벤트를 소유자에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_SUBSCRIBED,
      properties =
          "spring.json.value.default.type=io.mopl.core.event.playlist.PlaylistSubscribedEvent")
  public void handleSubscribed(PlaylistSubscribedEvent event) {
    try {
      UUID eventIdUuid = parseUuid(event.eventId(), "eventId", event.eventId());
      UUID ownerIdUuid = parseUuid(event.ownerId(), "ownerId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.playlist.subscribed.title",
              new Object[] {event.subscriberName()},
              "새 구독자: " + event.subscriberName(),
              Locale.KOREAN);

      Notification notification =
          Notification.builder()
              .eventId(eventIdUuid)
              .receiverId(ownerIdUuid)
              .title(title)
              .content("")
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

  // 플레이리스트 콘텐츠 추가 이벤트를 구독자에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_CONTENT_ADDED,
      properties =
          "spring.json.value.default.type=io.mopl.core.event.playlist.PlaylistContentAddedEvent")
  public void handleContentAdded(PlaylistContentAddedEvent event) {
    try {
      UUID playlistIdUuid = parseUuid(event.playlistId(), "playlistId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.playlist.content-added.title",
              null,
              "플레이리스트에 새 콘텐츠가 추가되었습니다.",
              Locale.KOREAN);

      List<UUID> receiverIds = recipientQuery.findSubscriberIds(playlistIdUuid);
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

  // 플레이리스트 생성 이벤트를 팔로워에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_CREATED,
      properties =
          "spring.json.value.default.type=io.mopl.core.event.playlist.PlaylistCreatedEvent")
  public void handleCreated(PlaylistCreatedEvent event) {
    try {
      UUID ownerIdUuid = parseUuid(event.ownerId(), "ownerId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.playlist.created.title",
              new Object[] {event.ownerName()},
              "새 플레이리스트가 생성되었습니다. 작성자: " + event.ownerName(),
              Locale.KOREAN);

      List<UUID> receiverIds = recipientQuery.findFollowerIds(ownerIdUuid);
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

  private UUID toPerReceiverEventId(String eventId, UUID receiverId) {
    String source = eventId + ":" + receiverId;
    return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
  }
}
