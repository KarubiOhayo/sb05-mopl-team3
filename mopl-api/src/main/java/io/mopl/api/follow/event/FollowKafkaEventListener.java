package io.mopl.api.follow.event;

import io.mopl.core.event.follow.UserFollowedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FollowKafkaEventListener {

  private final FollowEventPublisher followEventPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onFollowCreated(FollowCreatedInternalEvent e) {
    UserFollowedEvent event =
        new UserFollowedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            e.followerId().toString(),
            e.followerName(),
            e.followeeId().toString());
    followEventPublisher.publish(event);
  }
}
