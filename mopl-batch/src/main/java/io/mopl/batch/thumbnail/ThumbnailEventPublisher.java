package io.mopl.batch.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** 썸네일 생성 요청 이벤트를 Kafka로 발행한다. */
@Component
@RequiredArgsConstructor
public class ThumbnailEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * 썸네일 생성 요청 이벤트를 발행한다.
   *
   * @param event 썸네일 요청 이벤트
   */
  public void publishRequested(ContentThumbnailRequestedEvent event) {
    kafkaTemplate.send(KafkaTopics.CONTENT_THUMBNAIL_REQUESTED, event.contentId(), event);
  }
}
