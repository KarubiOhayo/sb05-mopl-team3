package io.mopl.batch.content;

import io.mopl.batch.common.UuidV7Generator;
import io.mopl.batch.common.event.ContentIndexBatchSpringEvent;
import io.mopl.core.event.content.ContentIndexBatchRequestedEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentIndexKafkaEventListener {

  private final ContentIndexEventPublisher publisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ContentIndexBatchSpringEvent event) {
    log.info("ContentIndexBatchSpringEvent 수신 : event={}, ", event);
    ContentIndexBatchRequestedEvent kafkaEvent =
        new ContentIndexBatchRequestedEvent(
            UuidV7Generator.generate().toString(), Instant.now(), event.contentIds(), 0);

    log.info("카프카 이벤트 발행: kafkaEvent={}, ", kafkaEvent);
    publisher.publish(kafkaEvent);
  }
}
