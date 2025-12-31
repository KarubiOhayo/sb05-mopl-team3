package io.mopl.worker.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailCompletedEvent;
import io.mopl.core.event.thumbnail.ContentThumbnailFailedEvent;
import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentThumbnailRequestedListener {

  private final ThumbnailS3Uploader thumbnailS3Uploader;
  private final ThumbnailEventPublisher thumbnailEventPublisher;

  @KafkaListener(topics = KafkaTopics.CONTENT_THUMBNAIL_REQUESTED)
  public void handle(ContentThumbnailRequestedEvent event) throws Exception {
    log.info(
        "Received thumbnail request: contentId={}, s3Key={}, sourceUrl={}, attempt={}",
        event.contentId(),
        event.s3Key(),
        event.sourceUrl(),
        event.attempt());

    if (event.sourceUrl() == null || event.sourceUrl().isBlank()) {
      log.warn("Skip thumbnail upload: sourceUrl is blank. contentId={}", event.contentId());
      return;
    }

    try {
      thumbnailS3Uploader.uploadFromUrl(event.sourceUrl(), event.s3Key());
      ContentThumbnailCompletedEvent completedEvent =
          new ContentThumbnailCompletedEvent(
              UUID.randomUUID().toString(),
              Instant.now(),
              event.contentId(),
              event.sourceType(),
              event.sourceUrl(),
              event.s3Key(),
              event.attempt());
      thumbnailEventPublisher.publishCompleted(completedEvent);
    } catch (Exception ex) {
      ContentThumbnailFailedEvent failedEvent =
          new ContentThumbnailFailedEvent(
              UUID.randomUUID().toString(),
              Instant.now(),
              event.contentId(),
              event.sourceType(),
              event.sourceUrl(),
              event.s3Key(),
              event.attempt(),
              ex.getMessage());
      thumbnailEventPublisher.publishFailed(failedEvent);
      log.error(
          "Thumbnail upload failed: contentId={}, s3Key={}, sourceUrl={}",
          event.contentId(),
          event.s3Key(),
          event.sourceUrl(),
          ex);
    }
  }
}
