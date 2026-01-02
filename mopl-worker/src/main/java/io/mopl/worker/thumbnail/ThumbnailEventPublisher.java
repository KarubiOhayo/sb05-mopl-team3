package io.mopl.worker.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailCompletedEvent;
import io.mopl.core.event.thumbnail.ContentThumbnailFailedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ThumbnailEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

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
