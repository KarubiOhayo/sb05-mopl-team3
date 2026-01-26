package io.mopl.worker.content.event;

import io.mopl.core.event.content.ContentAggregateUpdatedBatchEvent;
import io.mopl.core.event.content.ContentAggregateUpdatedEvent;
import io.mopl.core.event.content.ContentIndexBatchRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.content.index.ContentIndexEventPublisher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentAggregateUpdatedKafkaListener {

  private final ContentIndexEventPublisher contentIndexEventPublisher;

  @KafkaListener(
      topics = KafkaTopics.CONTENT_AGGREGATE_UPDATED,
      groupId = "content-aggregate-updated-worker",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.content.ContentAggregateUpdatedEvent")
  public void handle(ContentAggregateUpdatedEvent event) {
    UUID contentId;
    try {
      contentId = UUID.fromString(event.contentId());
    } catch (IllegalArgumentException e) {
      log.warn(
          "Invalid contentId on aggregate event: contentId={}, eventId={}",
          event.contentId(),
          event.eventId(),
          e);
      return;
    }

    ContentIndexBatchRequestedEvent indexEvent =
        new ContentIndexBatchRequestedEvent(event.eventId(), Instant.now(), List.of(contentId), 1);
    contentIndexEventPublisher.publish(indexEvent);
  }

  @KafkaListener(
      topics = KafkaTopics.CONTENT_AGGREGATE_UPDATED_BATCH,
      groupId = "content-aggregate-updated-worker",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.content.ContentAggregateUpdatedBatchEvent")
  public void handleBatch(ContentAggregateUpdatedBatchEvent event) {
    if (event.contentIds() == null || event.contentIds().isEmpty()) {
      return;
    }

    List<UUID> contentIds = new ArrayList<>();
    for (String contentIdRaw : event.contentIds()) {
      try {
        contentIds.add(UUID.fromString(contentIdRaw));
      } catch (IllegalArgumentException e) {
        log.warn(
            "Invalid contentId on aggregate batch event: contentId={}, eventId={}",
            contentIdRaw,
            event.eventId(),
            e);
      }
    }

    if (contentIds.isEmpty()) {
      return;
    }

    ContentIndexBatchRequestedEvent indexEvent =
        new ContentIndexBatchRequestedEvent(event.eventId(), Instant.now(), contentIds, 1);
    contentIndexEventPublisher.publish(indexEvent);
  }
}
