package io.mopl.worker.notification;

import io.mopl.core.event.watching.WatchingSessionStartedEvent;
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
public class WatchingSessionNotificationListener {

  private static final int BATCH_SIZE = 500;

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
      UUID watcherIdUuid =
          NotificationListenerSupport.parseUuid(
              log, event.watcherId(), "watcherId", event.eventId());
      UUID contentIdUuid =
          NotificationListenerSupport.parseUuid(
              log, event.contentId(), "contentId", event.eventId());

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
