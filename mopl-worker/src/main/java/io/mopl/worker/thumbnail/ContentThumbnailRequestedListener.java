package io.mopl.worker.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContentThumbnailRequestedListener {

  @KafkaListener(topics = KafkaTopics.CONTENT_THUMBNAIL_REQUESTED)
  public void handle(ContentThumbnailRequestedEvent event) {
    log.info(
        "Received thumbnail request: contentId={}, s3Key={}, sourceUrl={}, attempt={}",
        event.contentId(),
        event.s3Key(),
        event.sourceUrl(),
        event.attempt());
  }
}
