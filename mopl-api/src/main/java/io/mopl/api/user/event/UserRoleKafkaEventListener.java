package io.mopl.api.user.event;

import io.mopl.core.event.user.UserRoleChangedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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

    publisher.publish(kafkaEvent);
  }
}
