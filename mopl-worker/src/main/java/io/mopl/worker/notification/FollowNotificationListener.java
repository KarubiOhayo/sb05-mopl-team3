package io.mopl.worker.notification;

import io.mopl.core.event.follow.UserFollowedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.notification.domain.Notification;
import io.mopl.worker.notification.domain.NotificationLevel;
import io.mopl.worker.notification.domain.NotificationRepository;
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
public class FollowNotificationListener {

  private final NotificationRepository notificationRepository;
  private final MessageSource messageSource;
  private final NotificationEventPublisher notificationEventPublisher;
  private final NotificationMetrics notificationMetrics;

  @KafkaListener(
      topics = KafkaTopics.USER_FOLLOWED,
      properties = {
        "spring.json.value.default.type=io.mopl.core.event.follow.UserFollowedEvent",
        "max.poll.records=200",
        "fetch.max.wait.ms=200",
        "fetch.min.bytes=1"
      },
      containerFactory = "notificationBatchKafkaListenerContainerFactory")
  public void handle(List<UserFollowedEvent> events) {
    if (events == null || events.isEmpty()) {
      return;
    }

    List<Notification> notifications = new ArrayList<>(events.size());
    List<String> eventIds = new ArrayList<>(events.size());

    for (UserFollowedEvent event : events) {
      try {
        log.debug("팔로우 이벤트 수신: eventId={}", event.eventId());

        UUID eventIdUuid =
            NotificationListenerSupport.parseUuid(log, event.eventId(), "eventId", event.eventId());
        UUID followeeIdUuid =
            NotificationListenerSupport.parseUuid(
                log, event.followeeId(), "followeeId", event.eventId());

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
        notifications.add(notification);
        eventIds.add(event.eventId());
      } catch (IllegalArgumentException e) {
        notificationMetrics.recordFailure("follow", "invalid_payload");
      }
    }

    if (notifications.isEmpty()) {
      return;
    }

    try {
      List<Notification> saved = notificationRepository.saveAll(notifications);
      saved.forEach(notificationEventPublisher::publish);
    } catch (DataIntegrityViolationException e) {
      for (int i = 0; i < notifications.size(); i++) {
        Notification notification = notifications.get(i);
        String eventId = eventIds.get(i);
        try {
          Notification saved = notificationRepository.save(notification);
          notificationEventPublisher.publish(saved);
        } catch (DataIntegrityViolationException inner) {
          notificationMetrics.recordFailure(
              "follow", NotificationListenerSupport.classifyDataIntegrityViolation(inner));
          NotificationListenerSupport.handleDataIntegrityViolation(log, inner, eventId);
        }
      }
    }
  }
}
