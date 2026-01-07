package io.mopl.worker.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/** 썸네일 요청 Kafka 이벤트를 수신해 비동기 처리로 위임한다. */
@Component
@RequiredArgsConstructor
public class ContentThumbnailRequestedListener {

  private final ContentThumbnailRequestedHandler handler;

  /**
   * 썸네일 요청 이벤트를 수신한다.
   *
   * @param event 요청 이벤트
   * @param acknowledgment Kafka ACK
   */
  @KafkaListener(topics = KafkaTopics.CONTENT_THUMBNAIL_REQUESTED)
  public void handle(ContentThumbnailRequestedEvent event, Acknowledgment acknowledgment) {
    handler.handleAsync(event, acknowledgment);
  }
}
