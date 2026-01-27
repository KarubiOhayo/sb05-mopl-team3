package io.mopl.worker.notification;

import io.mopl.core.event.playlist.PlaylistContentAddedEvent;
import io.mopl.core.event.playlist.PlaylistCreatedEvent;
import io.mopl.core.event.playlist.PlaylistSubscribedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.notification.domain.Notification;
import io.mopl.worker.notification.domain.NotificationLevel;
import io.mopl.worker.notification.domain.NotificationRepository;
import java.time.Instant;
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
  private final NotificationMetrics notificationMetrics;

  // 플레이리스트 구독 이벤트를 소유자에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_SUBSCRIBED,
      properties = {
        "spring.json.value.default.type=io.mopl.core.event.playlist.PlaylistSubscribedEvent",
        "max.poll.records=200",
        "fetch.max.wait.ms=200",
        "fetch.min.bytes=1"
      },
      containerFactory = "notificationBatchKafkaListenerContainerFactory")
  public void handleSubscribed(List<PlaylistSubscribedEvent> events) {
    if (events == null || events.isEmpty()) {
      return;
    }
    for (PlaylistSubscribedEvent event : events) {
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
        notificationMetrics.recordFailure(
            "playlist_subscribed", NotificationListenerSupport.classifyDataIntegrityViolation(e));
        NotificationListenerSupport.handleDataIntegrityViolation(log, e, event.eventId());
      } catch (IllegalArgumentException e) {
        notificationMetrics.recordFailure("playlist_subscribed", "invalid_payload");
        // UUID 파싱 오류는 parseUuid에서 로그 처리한다.
      }
    }
  }

  // 플레이리스트 콘텐츠 추가 이벤트를 구독자에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_CONTENT_ADDED,
      properties = {
        "spring.json.value.default.type=io.mopl.core.event.playlist.PlaylistContentAddedEvent",
        "max.poll.records=200",
        "fetch.max.wait.ms=200",
        "fetch.min.bytes=1"
      },
      containerFactory = "notificationBatchKafkaListenerContainerFactory")
  public void handleContentAdded(List<PlaylistContentAddedEvent> events) {
    if (events == null || events.isEmpty()) {
      return;
    }
    for (PlaylistContentAddedEvent event : events) {
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

        Instant cursorCreatedAt = null;
        String cursorUserId = null;

        while (true) {
          NotificationRecipientQuery.RecipientPage page =
              recipientQuery.findSubscriberIdsPage(
                  playlistIdUuid, cursorCreatedAt, cursorUserId, BATCH_SIZE);

          if (!page.receiverIds().isEmpty()) {
            List<Notification> notifications = new ArrayList<>(page.receiverIds().size());
            for (UUID receiverId : page.receiverIds()) {
              // 수신자별로 event_id를 분리해 중복 충돌을 방지한다.
              UUID eventIdUuid =
                  NotificationListenerSupport.toPerReceiverEventId(event.eventId(), receiverId);
              notifications.add(
                  Notification.builder()
                      .eventId(eventIdUuid)
                      .receiverId(receiverId)
                      .title(title)
                      .content("")
                      .level(NotificationLevel.INFO)
                      .build());
            }
            NotificationListenerSupport.saveAndPublishBatch(
                log,
                notifications,
                event.eventId(),
                notificationRepository,
                notificationEventPublisher,
                notificationMetrics,
                "playlist_content_added");
          }

          if (!page.hasNext() || page.nextCreatedAt() == null || page.nextCursorId() == null) {
            break;
          }
          cursorCreatedAt = page.nextCreatedAt();
          cursorUserId = page.nextCursorId();
        }
      } catch (IllegalArgumentException e) {
        notificationMetrics.recordFailure("playlist_content_added", "invalid_payload");
        // UUID 파싱 오류는 parseUuid에서 로그 처리한다.
      }
    }
  }

  // 플레이리스트 생성 이벤트를 팔로워에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.PLAYLIST_CREATED,
      properties = {
        "spring.json.value.default.type=io.mopl.core.event.playlist.PlaylistCreatedEvent",
        "max.poll.records=200",
        "fetch.max.wait.ms=200",
        "fetch.min.bytes=1"
      },
      containerFactory = "notificationBatchKafkaListenerContainerFactory")
  public void handleCreated(List<PlaylistCreatedEvent> events) {
    if (events == null || events.isEmpty()) {
      return;
    }
    for (PlaylistCreatedEvent event : events) {
      try {
        UUID ownerIdUuid =
            NotificationListenerSupport.parseUuid(log, event.ownerId(), "ownerId", event.eventId());

        String title =
            messageSource.getMessage(
                "notification.playlist.created.title",
                new Object[] {event.ownerName()},
                event.ownerName() + "님이 새 플레이리스트를 만들었어요.",
                Locale.KOREAN);

        Instant cursorCreatedAt = null;
        String cursorId = null;

        while (true) {
          NotificationRecipientQuery.RecipientPage page =
              recipientQuery.findFollowerIdsPage(
                  ownerIdUuid, cursorCreatedAt, cursorId, BATCH_SIZE);

          if (!page.receiverIds().isEmpty()) {
            List<Notification> notifications = new ArrayList<>(page.receiverIds().size());
            for (UUID receiverId : page.receiverIds()) {
              // 수신자별로 event_id를 분리해 중복 충돌을 방지한다.
              UUID eventIdUuid =
                  NotificationListenerSupport.toPerReceiverEventId(event.eventId(), receiverId);
              notifications.add(
                  Notification.builder()
                      .eventId(eventIdUuid)
                      .receiverId(receiverId)
                      .title(title)
                      .content("")
                      .level(NotificationLevel.INFO)
                      .build());
            }
            NotificationListenerSupport.saveAndPublishBatch(
                log,
                notifications,
                event.eventId(),
                notificationRepository,
                notificationEventPublisher,
                notificationMetrics,
                "playlist_created");
          }

          if (!page.hasNext() || page.nextCreatedAt() == null || page.nextCursorId() == null) {
            break;
          }
          cursorCreatedAt = page.nextCreatedAt();
          cursorId = page.nextCursorId();
        }
      } catch (IllegalArgumentException e) {
        notificationMetrics.recordFailure("playlist_created", "invalid_payload");
        // UUID 파싱 오류는 parseUuid에서 로그 처리한다.
      }
    }
  }
}
