package io.mopl.batch.thumbnail;

import io.mopl.batch.common.UuidV7Generator;
import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** DB 트랜잭션 커밋 후 썸네일 요청 이벤트를 Kafka 이벤트로 변환해 발행한다. */
@Component
@RequiredArgsConstructor
public class ThumbnailKafkaEventListener {

  private final ThumbnailEventPublisher thumbnailEventPublisher;

  /**
   * 내부 스프링 이벤트를 Kafka 이벤트로 변환하여 발행한다.
   *
   * @param event 썸네일 요청 스프링 이벤트
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ThumbnailRequestedSpringEvent event) {
    if (event.uploadMode() != ThumbnailUploadMode.ASYNC) {
      return;
    }
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
