package io.mopl.worker.content;

import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.core.event.content.ContentAggregateUpdatedEvent;
import io.mopl.core.event.review.ReviewUpdatedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.content.event.ContentAggregateEventPublisher;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewUpdatedEventListener {

  private final ContentAggregateRepository contentAggregateRepository;
  private final ContentAggregateEventPublisher contentAggregateEventPublisher;

  @Transactional
  @KafkaListener(
      topics = KafkaTopics.REVIEW_UPDATED,
      properties = "spring.json.value.default.type=io.mopl.core.event.review.ReviewUpdatedEvent")
  public void handle(ReviewUpdatedEvent event) {
    log.info(
        "리뷰 이벤트 수신: eventId={}, contentId={}, beforeRating={}, afterRating={}",
        event.eventId(),
        event.contentId(),
        event.beforeRating(),
        event.afterRating());
    UUID contentId;
    try {
      contentId = UUID.fromString(event.contentId());
    } catch (IllegalArgumentException e) {
      log.error(
          "리뷰 이벤트 UUID 파싱 실패: eventId={}, contentId={}", event.eventId(), event.contentId(), e);
      return;
    }

    int updated =
        contentAggregateRepository.updateReview(
            contentId, event.beforeRating(), event.afterRating());
    if (updated == 0) {
      log.warn("콘텐츠 업데이트 실패 (contentId={}, eventId={})", event.contentId(), event.eventId());
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
    log.info("콘텐츠 업데이트 완료 (contentId={}, eventId={})", event.contentId(), event.eventId());

    contentAggregateEventPublisher.publish(
        new ContentAggregateUpdatedEvent(
            UUID.randomUUID().toString(), Instant.now(), event.contentId()));
    log.info("집계 갱신 이벤트 발행 (contentId={}, eventId={})", event.contentId(), event.eventId());
  }
}
