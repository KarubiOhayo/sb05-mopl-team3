package io.mopl.worker.notification;

import io.mopl.core.event.user.UserRoleChangedEvent;
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
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRoleNotificationListener {

  private final NotificationRepository notificationRepository;
  private final MessageSource messageSource;
  private final NotificationEventPublisher notificationEventPublisher;

  // 사용자 권한 변경 이벤트를 수신해 알림을 저장한다.
  @KafkaListener(
      topics = KafkaTopics.USER_ROLE_CHANGED,
      properties = "spring.json.value.default.type=io.mopl.core.event.user.UserRoleChangedEvent")
  public void handle(UserRoleChangedEvent event) {
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
      Notification saved = notificationRepository.save(notification);
      notificationEventPublisher.publish(saved);
    } catch (DataIntegrityViolationException e) {
      NotificationListenerSupport.handleDataIntegrityViolation(log, e, event.eventId());
    } catch (IllegalArgumentException e) {
      // UUID 파싱 오류는 parseUuid에서 로그 처리.
    }
  }
}
