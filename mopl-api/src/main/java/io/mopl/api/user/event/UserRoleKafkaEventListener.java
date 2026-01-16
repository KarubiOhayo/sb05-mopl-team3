package io.mopl.api.user.event;

import io.mopl.core.event.user.UserRoleChangedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRoleKafkaEventListener {

  private final UserRoleEventPublisher publisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRoleChanged(UserRoleChangedInternalEvent event) {
    UserRoleChangedEvent kafkaEvent =
        new UserRoleChangedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            event.userId().toString(),
            event.userName(),
            event.newRole());

    try {
      publisher.publish(kafkaEvent);
    } catch (Exception e) {
      log.error("권한 변경 이벤트 Kafka 발행 실패 - userId: {}", event.userId(), e);
    }
  }
}
