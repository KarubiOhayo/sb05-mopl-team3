package io.mopl.worker.content;

import io.mopl.core.db.DbConstraintNames;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.core.event.content.ContentAggregateUpdatedEvent;
import io.mopl.core.event.review.ReviewCreatedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.content.domain.ProcessedEvent;
import io.mopl.worker.content.domain.ProcessedEventRepository;
import io.mopl.worker.content.event.ContentAggregateEventPublisher;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewCreatedEventListener {

  private final ProcessedEventRepository processedEventRepository;
  private final ContentAggregateRepository contentAggregateRepository;
  private final ContentAggregateEventPublisher contentAggregateEventPublisher;

  @Transactional
  @KafkaListener(
      topics = KafkaTopics.REVIEW_CREATED,
      properties = "spring.json.value.default.type=io.mopl.core.event.review.ReviewCreatedEvent")
  public void handle(ReviewCreatedEvent event) {
    log.info(
        "리뷰 이벤트 수신: eventId={}, contentId={}, rating={}",
        event.eventId(),
        event.contentId(),
        event.rating());
    UUID eventId;
    UUID contentId;
    try {
      eventId = UUID.fromString(event.eventId());
      contentId = UUID.fromString(event.contentId());
    } catch (IllegalArgumentException e) {
      log.error(
          "리뷰 이벤트 UUID 파싱 실패: eventId={}, contentId={}", event.eventId(), event.contentId(), e);
      return;
    }

    try {
      processedEventRepository.save(
          ProcessedEvent.builder().eventId(eventId).processedAt(Instant.now()).build());
    } catch (DataIntegrityViolationException e) {
      Throwable cause = e.getMostSpecificCause();
      String message = cause != null ? cause.getMessage() : e.getMessage();
      if (message != null && message.contains(DbConstraintNames.UQ_PROCESSED_EVENTS_EVENT_ID)) {
        log.debug("중복 이벤트 무시 (eventId={})", event.eventId());
        return;
      }
      log.error("processed_events 저장 실패 (eventId={})", event.eventId(), e);
      return;
    }

    int updated = contentAggregateRepository.applyReview(contentId, event.rating());
    if (updated == 0) {
      log.warn("콘텐츠 집계 업데이트 실패 (contentId={}, eventId={})", event.contentId(), event.eventId());
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
    log.info("콘텐츠 집계 업데이트 완료 (contentId={}, eventId={})", event.contentId(), event.eventId());

    contentAggregateEventPublisher.publish(
        new ContentAggregateUpdatedEvent(
            UUID.randomUUID().toString(), Instant.now(), event.contentId()));
    log.info("집계 갱신 이벤트 발행 (contentId={}, eventId={})", event.contentId(), event.eventId());
  }
}
