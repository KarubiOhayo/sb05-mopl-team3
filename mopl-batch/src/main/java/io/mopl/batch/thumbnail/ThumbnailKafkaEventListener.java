package io.mopl.batch.thumbnail;

import io.mopl.batch.common.UuidV7Generator;
import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ThumbnailKafkaEventListener {

  private final ThumbnailEventPublisher thumbnailEventPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ThumbnailRequestedSpringEvent event) {
    ContentThumbnailRequestedEvent kafkaEvent =
        new ContentThumbnailRequestedEvent(
            UuidV7Generator.generate().toString(),
            Instant.now(),
            event.contentId(),
            event.sourceType(),
            event.sourceUrl(),
            event.s3Key(),
            0);

    thumbnailEventPublisher.publishRequested(kafkaEvent);
  }
}
