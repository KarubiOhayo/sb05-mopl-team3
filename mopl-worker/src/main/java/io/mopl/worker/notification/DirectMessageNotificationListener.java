package io.mopl.worker.notification;

import io.mopl.core.event.dm.DirectMessageReceivedEvent;
import io.mopl.core.event.notification.NotificationType;
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
public class DirectMessageNotificationListener {

  private final NotificationRepository notificationRepository;
  private final MessageSource messageSource;
  private final NotificationEventPublisher notificationEventPublisher;
  private final DirectMessageActiveConversationTracker activeConversationTracker;
  private final NotificationMetrics notificationMetrics;

  // DM 수신 이벤트를 수신해 알림을 저장한다.
  @KafkaListener(
      topics = KafkaTopics.DIRECT_MESSAGE_RECEIVED,
      concurrency = "3",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.dm.DirectMessageReceivedEvent")
  @Async("kafkaTaskExecutor")
  public void handle(DirectMessageReceivedEvent event) {
    try {
      UUID eventIdUuid =
          NotificationListenerSupport.parseUuid(log, event.eventId(), "eventId", event.eventId());
      UUID receiverIdUuid =
          NotificationListenerSupport.parseUuid(
              log, event.receiverId(), "receiverId", event.eventId());
      UUID conversationIdUuid =
          NotificationListenerSupport.parseUuid(
              log, event.conversationId(), "conversationId", event.eventId());

      if (activeConversationTracker.isActive(receiverIdUuid, conversationIdUuid)) {
        log.info(
            "활성 대화는 DM 알림 저장을 건너뜁니다: receiverId={}, conversationId={}",
            receiverIdUuid,
            conversationIdUuid);
        return;
      }

      String title =
          messageSource.getMessage(
              "notification.dm.received.title",
              new Object[] {event.senderName()},
              "새 메시지가 도착했습니다. 보낸 사람: " + event.senderName(),
              Locale.KOREAN);

      String content = event.content() != null ? event.content() : "";

      Notification notification =
          Notification.builder()
              .eventId(eventIdUuid)
              .receiverId(receiverIdUuid)
              .title(title)
              .content(content)
              .level(NotificationLevel.INFO)
              .build();
      Notification saved = notificationRepository.save(notification);
      notificationEventPublisher.publish(
          saved, NotificationType.DIRECT_MESSAGE, event.conversationId());
    } catch (DataIntegrityViolationException e) {
      notificationMetrics.recordFailure(
          "direct_message", NotificationListenerSupport.classifyDataIntegrityViolation(e));
      NotificationListenerSupport.handleDataIntegrityViolation(log, e, event.eventId());
    } catch (IllegalArgumentException e) {
      notificationMetrics.recordFailure("direct_message", "invalid_payload");
      // UUID 파싱 오류는 parseUuid에서 로그 처리.
    }
  }
}
