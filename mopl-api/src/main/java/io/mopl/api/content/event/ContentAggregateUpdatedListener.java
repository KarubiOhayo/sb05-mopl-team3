package io.mopl.api.content.event;

import io.mopl.api.content.domain.EventType;
import io.mopl.core.event.content.ContentAggregateUpdatedEvent;
import io.mopl.core.kafka.KafkaTopics;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentAggregateUpdatedListener {

  private final ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  @KafkaListener(
      topics = KafkaTopics.CONTENT_AGGREGATE_UPDATED,
      groupId = "content-aggregate-updated",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.content.ContentAggregateUpdatedEvent")
  public void handle(ContentAggregateUpdatedEvent event) {
    log.info("집계 갱신 이벤트 수신: eventId={}, contentId={}", event.eventId(), event.contentId());
    UUID contentId;
    try {
      contentId = UUID.fromString(event.contentId());
    } catch (IllegalArgumentException e) {
      log.error("contentId UUID 파싱 실패: {} (eventId={})", event.contentId(), event.eventId(), e);
      return;
    }

    applicationEventPublisher.publishEvent(new ContentIndexEvent(contentId, EventType.UPSERT));
    log.info("ES 인덱싱 이벤트 발행 (contentId={})", contentId);
  }
}
