package io.mopl.worker.notification;

import io.mopl.core.event.follow.UserFollowedEvent;
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
import org.springframework.scheduling.annotation.Async;
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
      properties = "spring.json.value.default.type=io.mopl.core.event.follow.UserFollowedEvent")
  @Async("kafkaTaskExecutor")
  public void handle(UserFollowedEvent event) {
    try {
      log.info("팔로우 이벤트 수신: eventId={}", event.eventId());

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
      Notification saved = notificationRepository.save(notification);
      notificationEventPublisher.publish(saved);
    } catch (DataIntegrityViolationException e) {
      notificationMetrics.recordFailure(
          "follow", NotificationListenerSupport.classifyDataIntegrityViolation(e));
      // 이미 처리된 이벤트는 무시한다.
      NotificationListenerSupport.handleDataIntegrityViolation(log, e, event.eventId());
    } catch (IllegalArgumentException e) {
      notificationMetrics.recordFailure("follow", "invalid_payload");
      // UUID 파싱 오류는 parseUuid에서 로그 처리.
    }
  }
}
