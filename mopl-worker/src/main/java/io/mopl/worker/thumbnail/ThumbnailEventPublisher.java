package io.mopl.worker.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailCompletedEvent;
import io.mopl.core.event.thumbnail.ContentThumbnailFailedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThumbnailEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publishCompleted(ContentThumbnailCompletedEvent event) {
    kafkaTemplate.send(KafkaTopics.CONTENT_THUMBNAIL_COMPLETED, event.contentId(), event);
  }

  public void publishFailed(ContentThumbnailFailedEvent event) {
    kafkaTemplate.send(KafkaTopics.CONTENT_THUMBNAIL_FAILED, event.contentId(), event);
  }
}
