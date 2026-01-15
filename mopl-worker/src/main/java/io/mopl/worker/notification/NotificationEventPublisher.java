package io.mopl.worker.notification;

import io.mopl.core.event.notification.NotificationCreatedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  // 알림 저장 결과를 SSE 전송용 이벤트로 발행한다.
  public void publish(Notification notification) {
    NotificationCreatedEvent event =
        new NotificationCreatedEvent(
            notification.getId().toString(),
            notification.getCreatedAt(),
            notification.getId().toString(),
            notification.getReceiverId().toString(),
            notification.getTitle(),
            notification.getContent(),
            notification.getLevel().name());

    kafkaTemplate.send(
        KafkaTopics.NOTIFICATION_CREATED, notification.getReceiverId().toString(), event);
  }
}
