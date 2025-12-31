package io.mopl.worker.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentThumbnailRequestedListener {

  private final ContentThumbnailRequestedHandler handler;

  @KafkaListener(topics = KafkaTopics.CONTENT_THUMBNAIL_REQUESTED)
  public void handle(ContentThumbnailRequestedEvent event, Acknowledgment acknowledgment) {
    handler.handleAsync(event, acknowledgment);
  }
}
