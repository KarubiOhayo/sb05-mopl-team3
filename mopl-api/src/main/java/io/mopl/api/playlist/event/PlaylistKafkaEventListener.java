package io.mopl.api.playlist.event;

import io.mopl.core.event.playlist.PlaylistContentAddedEvent;
import io.mopl.core.event.playlist.PlaylistCreatedEvent;
import io.mopl.core.event.playlist.PlaylistSubscribedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PlaylistKafkaEventListener {

  private final PlaylistEventPublisher publisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onCreated(PlaylistCreatedInternalEvent e) {
    publisher.publishCreated(
        new PlaylistCreatedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            e.playlistId().toString(),
            e.ownerId().toString(),
            e.ownerName()));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onSubscribed(PlaylistSubscribedInternalEvent e) {
    publisher.publishSubscribed(
        new PlaylistSubscribedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            e.playlistId().toString(),
            e.ownerId().toString(),
            e.subscriberId().toString(),
            e.subscriberName()));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onContentAdded(PlaylistContentAddedInternalEvent e) {
    publisher.publishContentAdded(
        new PlaylistContentAddedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            e.playlistId().toString(),
            e.ownerId().toString(),
            e.contentId().toString()));
  }
}
