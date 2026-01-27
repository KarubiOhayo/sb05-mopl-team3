package io.mopl.worker.notification;

import io.mopl.core.event.user.UserRoleChangedEvent;
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
public class UserRoleNotificationListener {

  private final NotificationRepository notificationRepository;
  private final MessageSource messageSource;
  private final NotificationEventPublisher notificationEventPublisher;
  private final NotificationMetrics notificationMetrics;

  // 사용자 권한 변경 이벤트를 수신해 알림을 저장한다.
  @KafkaListener(
      topics = KafkaTopics.USER_ROLE_CHANGED,
      properties = {
        "spring.json.value.default.type=io.mopl.core.event.user.UserRoleChangedEvent",
        "max.poll.records=200",
        "fetch.max.wait.ms=200",
        "fetch.min.bytes=1"
      },
      containerFactory = "notificationBatchKafkaListenerContainerFactory")
  public void handle(List<UserRoleChangedEvent> events) {
    if (events == null || events.isEmpty()) {
      return;
    }

    List<Notification> notifications = new ArrayList<>(events.size());
    List<String> eventIds = new ArrayList<>(events.size());

    for (UserRoleChangedEvent event : events) {
      try {
        UUID eventIdUuid =
            NotificationListenerSupport.parseUuid(log, event.eventId(), "eventId", event.eventId());
        UUID userIdUuid =
            NotificationListenerSupport.parseUuid(log, event.userId(), "userId", event.eventId());

        String title =
            messageSource.getMessage(
                "notification.user.role-changed.title",
                new Object[] {event.newRole()},
                "권한이 " + event.newRole() + "(으)로 변경되었습니다.",
                Locale.KOREAN);

        Notification notification =
            Notification.builder()
                .eventId(eventIdUuid)
                .receiverId(userIdUuid)
                .title(title)
                .content("")
                .level(NotificationLevel.INFO)
                .build();
        notifications.add(notification);
        eventIds.add(event.eventId());
      } catch (IllegalArgumentException e) {
        notificationMetrics.recordFailure("user_role_changed", "invalid_payload");
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
              "user_role_changed",
              NotificationListenerSupport.classifyDataIntegrityViolation(inner));
          NotificationListenerSupport.handleDataIntegrityViolation(log, inner, eventId);
        }
      }
    }
  }
}
