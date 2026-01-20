package io.mopl.worker.notification;

import io.mopl.core.event.playlist.PlaylistContentAddedEvent;
import io.mopl.core.event.playlist.PlaylistCreatedEvent;
import io.mopl.core.event.playlist.PlaylistSubscribedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.notification.domain.Notification;
import io.mopl.worker.notification.domain.NotificationLevel;
import io.mopl.worker.notification.domain.NotificationRepository;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

  private static final int BATCH_SIZE = 500;

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
      UUID eventIdUuid =
          NotificationListenerSupport.parseUuid(log, event.eventId(), "eventId", event.eventId());
      UUID ownerIdUuid =
          NotificationListenerSupport.parseUuid(log, event.ownerId(), "ownerId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.playlist.subscribed.title",
              new Object[] {event.subscriberName()},
              event.subscriberName() + "님이 플레이리스트를 구독했습니다.",
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
      NotificationListenerSupport.handleDataIntegrityViolation(log, e, event.eventId());
    } catch (IllegalArgumentException e) {
      // UUID 파싱 오류는 parseUuid에서 로그 처리한다.
    }
  }

  // 플레이리스트 콘텐츠 추가 이벤트를 구독자에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_CONTENT_ADDED,
      properties =
          "spring.json.value.default.type=io.mopl.core.event.playlist.PlaylistContentAddedEvent")
  public void handleContentAdded(PlaylistContentAddedEvent event) {
    try {
      UUID playlistIdUuid =
          NotificationListenerSupport.parseUuid(
              log, event.playlistId(), "playlistId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.playlist.content-added.title",
              null,
              "플레이리스트에 콘텐츠가 추가되었습니다.",
              Locale.KOREAN);

      List<UUID> receiverIds = recipientQuery.findSubscriberIds(playlistIdUuid);
      for (int start = 0; start < receiverIds.size(); start += BATCH_SIZE) {
        List<UUID> batch =
            receiverIds.subList(start, Math.min(start + BATCH_SIZE, receiverIds.size()));
        List<Notification> notifications = new ArrayList<>(batch.size());
        for (UUID receiverId : batch) {
          // 수신자별로 event_id를 분리해 중복 충돌을 방지한다.
          UUID eventIdUuid = toPerReceiverEventId(event.eventId(), receiverId);
          notifications.add(
              Notification.builder()
                  .eventId(eventIdUuid)
                  .receiverId(receiverId)
                  .title(title)
                  .content("")
                  .level(NotificationLevel.INFO)
                  .build());
        }
        saveAndPublishBatch(notifications, event.eventId());
      }
    } catch (IllegalArgumentException e) {
      // UUID 파싱 오류는 parseUuid에서 로그 처리한다.
    }
  }

  // 플레이리스트 생성 이벤트를 팔로워에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_CREATED,
      properties =
          "spring.json.value.default.type=io.mopl.core.event.playlist.PlaylistCreatedEvent")
  public void handleCreated(PlaylistCreatedEvent event) {
    try {
      UUID ownerIdUuid =
          NotificationListenerSupport.parseUuid(log, event.ownerId(), "ownerId", event.eventId());

      String title =
          messageSource.getMessage(
              "notification.playlist.created.title",
              new Object[] {event.ownerName()},
              event.ownerName() + "님이 새 플레이리스트를 만들었어요.",
              Locale.KOREAN);

      List<UUID> receiverIds = recipientQuery.findFollowerIds(ownerIdUuid);
      for (int start = 0; start < receiverIds.size(); start += BATCH_SIZE) {
        List<UUID> batch =
            receiverIds.subList(start, Math.min(start + BATCH_SIZE, receiverIds.size()));
        List<Notification> notifications = new ArrayList<>(batch.size());
        for (UUID receiverId : batch) {
          // 수신자별로 event_id를 분리해 중복 충돌을 방지한다.
          UUID eventIdUuid = toPerReceiverEventId(event.eventId(), receiverId);
          notifications.add(
              Notification.builder()
                  .eventId(eventIdUuid)
                  .receiverId(receiverId)
                  .title(title)
                  .content("")
                  .level(NotificationLevel.INFO)
                  .build());
        }
        saveAndPublishBatch(notifications, event.eventId());
      }
    } catch (IllegalArgumentException e) {
      // UUID 파싱 오류는 parseUuid에서 로그 처리한다.
    }
  }

  private void saveAndPublishBatch(List<Notification> notifications, String eventId) {
    if (notifications.isEmpty()) {
      return;
    }

    try {
      List<Notification> saved = notificationRepository.saveAll(notifications);
      saved.forEach(notificationEventPublisher::publish);
    } catch (DataIntegrityViolationException ex) {
      for (Notification notification : notifications) {
        try {
          Notification saved = notificationRepository.save(notification);
          notificationEventPublisher.publish(saved);
        } catch (DataIntegrityViolationException inner) {
          NotificationListenerSupport.handleDataIntegrityViolation(
              log, inner, eventId + ":" + notification.getReceiverId());
        }
      }
    }
  }

  private UUID toPerReceiverEventId(String eventId, UUID receiverId) {
    String source = eventId + ":" + receiverId;
    return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
  }
}
