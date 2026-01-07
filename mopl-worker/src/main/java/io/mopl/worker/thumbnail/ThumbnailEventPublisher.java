package io.mopl.worker.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailCompletedEvent;
import io.mopl.core.event.thumbnail.ContentThumbnailFailedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** 썸네일 처리 결과 이벤트를 Kafka로 발행한다. */
@Component
@Slf4j
@RequiredArgsConstructor
public class ThumbnailEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * 썸네일 완료 이벤트를 발행한다.
   *
   * @param event 완료 이벤트
   */
  public void publishCompleted(ContentThumbnailCompletedEvent event) {
    kafkaTemplate
        .send(KafkaTopics.CONTENT_THUMBNAIL_COMPLETED, event.contentId(), event)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error(
                    "썸네일 완료 이벤트 발행 실패: contentId={}, eventId={}",
                    event.contentId(),
                    event.eventId(),
                    ex);
              }
            });
  }

  /**
   * 썸네일 실패 이벤트를 발행한다.
   *
   * @param event 실패 이벤트
   */
  public void publishFailed(ContentThumbnailFailedEvent event) {
    kafkaTemplate
        .send(KafkaTopics.CONTENT_THUMBNAIL_FAILED, event.contentId(), event)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error(
                    "썸네일 실패 이벤트 발행 실패: contentId={}, eventId={}",
                    event.contentId(),
                    event.eventId(),
                    ex);
              }
            });
  }
}
