package io.mopl.worker.notification;

import io.mopl.core.event.follow.UserFollowedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.notification.domain.Notification;
import io.mopl.worker.notification.domain.NotificationLevel;
import io.mopl.worker.notification.domain.NotificationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowNotificationListener {

  private final NotificationRepository notificationRepository;

  @KafkaListener(topics = KafkaTopics.USER_FOLLOWED)
  public void handle(UserFollowedEvent event, Acknowledgment acknowledgment) {
    try {
      log.info("follow event received: eventId={}", event.eventId());
      Notification notification =
          Notification.builder()
              .eventId(UUID.fromString(event.eventId()))
              .receiverId(UUID.fromString(event.followeeId()))
              .title(event.followerName() + " 님이 나를 팔로우했어요.")
              .content("")
              .level(NotificationLevel.INFO)
              .build();
      notificationRepository.save(notification);
    } catch (DataIntegrityViolationException e) {
      // duplicate, ignore
    } catch (IllegalArgumentException e) {
      log.error(
          "Invalid UUID format: eventId={}, followeeId={}", event.eventId(), event.followeeId(), e);
    } finally {
      acknowledgment.acknowledge();
    }
  }
}
