package io.mopl.batch.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThumbnailEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publishRequested(ContentThumbnailRequestedEvent event) {
    kafkaTemplate.send(KafkaTopics.CONTENT_THUMBNAIL_REQUESTED, event.contentId(), event);
  }
}
