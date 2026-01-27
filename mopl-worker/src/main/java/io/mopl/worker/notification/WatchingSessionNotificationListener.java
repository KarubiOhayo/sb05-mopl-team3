package io.mopl.worker.notification;

import io.mopl.core.event.watching.WatchingSessionStartedEvent;
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
  private final NotificationMetrics notificationMetrics;

  // 시청 시작 이벤트를 팔로워에게 알림으로 저장한다.
  @KafkaListener(
      topics = KafkaTopics.WATCHING_SESSION_STARTED,
      properties = {
        "spring.json.value.default.type=io.mopl.core.event.watching.WatchingSessionStartedEvent",
        "max.poll.records=200",
        "fetch.max.wait.ms=200",
        "fetch.min.bytes=1"
      },
      containerFactory = "notificationBatchKafkaListenerContainerFactory")
  public void handle(List<WatchingSessionStartedEvent> events) {
    if (events == null || events.isEmpty()) {
      return;
    }
    for (WatchingSessionStartedEvent event : events) {
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

        Instant cursorCreatedAt = null;
        String cursorId = null;

        while (true) {
          NotificationRecipientQuery.RecipientPage page =
              recipientQuery.findFollowerIdsPage(
                  watcherIdUuid, cursorCreatedAt, cursorId, BATCH_SIZE);

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
                "watching_session_started");
          }

          if (!page.hasNext() || page.nextCreatedAt() == null || page.nextCursorId() == null) {
            break;
          }
          cursorCreatedAt = page.nextCreatedAt();
          cursorId = page.nextCursorId();
        }
      } catch (IllegalArgumentException e) {
        notificationMetrics.recordFailure("watching_session_started", "invalid_payload");
        // UUID 파싱 오류는 parseUuid에서 로그 처리된다.
      }
    }
  }
}
