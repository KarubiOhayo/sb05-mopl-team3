package io.mopl.worker.thumbnail;

import io.mopl.core.event.thumbnail.ContentThumbnailCompletedEvent;
import io.mopl.core.event.thumbnail.ContentThumbnailFailedEvent;
import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.config.KafkaRetryProperties;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentThumbnailRequestedHandler {

  private final ThumbnailS3Uploader thumbnailS3Uploader;
  private final ThumbnailEventPublisher thumbnailEventPublisher;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaRetryProperties retryProperties;

  @Async("kafkaTaskExecutor")
  public void handleAsync(ContentThumbnailRequestedEvent event, Acknowledgment acknowledgment) {
    int maxAttempts = retryProperties.maxAttempts() == null ? 3 : retryProperties.maxAttempts();
    long backoffMs =
        retryProperties.initialBackoffMs() == null ? 1000L : retryProperties.initialBackoffMs();
    double backoffMultiplier =
        retryProperties.backoffMultiplier() == null ? 2.0 : retryProperties.backoffMultiplier();
    long maxBackoffMs =
        retryProperties.maxBackoffMs() == null ? 10000L : retryProperties.maxBackoffMs();

    try {
      log.info(
          "썸네일 요청 수신: contentId={}, s3Key={}, sourceUrl={}, attempt={}",
          event.contentId(),
          event.s3Key(),
          event.sourceUrl(),
          event.attempt());

      if (event.sourceUrl() == null || event.sourceUrl().isBlank()) {
        log.warn("썸네일 업로드 건너뜀: sourceUrl이 비어 있습니다. contentId={}", event.contentId());
        return;
      }

      Exception lastFailure = null;
      long currentBackoff = backoffMs;
      for (int attempt = 1; attempt <= maxAttempts; attempt++) {
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
          log.info(
              "썸네일 업로드 완료: contentId={}, s3Key={}, sourceUrl={}",
              event.contentId(),
              event.s3Key(),
              event.sourceUrl());
          return;
        } catch (Exception ex) {
          lastFailure = ex;
          log.warn(
              "썸네일 업로드 실패 (attempt {}/{}): contentId={}, s3Key={}",
              attempt,
              maxAttempts,
              event.contentId(),
              event.s3Key(),
              ex);
          if (attempt < maxAttempts) {
            try {
              Thread.sleep(currentBackoff);
            } catch (InterruptedException interruptedException) {
              Thread.currentThread().interrupt();
              break;
            }
            currentBackoff =
                Math.min(
                    maxBackoffMs, Math.max(1L, Math.round(currentBackoff * backoffMultiplier)));
          }
        }
      }

      String errorMessage = lastFailure == null ? "unknown error" : lastFailure.getMessage();
      ContentThumbnailFailedEvent failedEvent =
          new ContentThumbnailFailedEvent(
              UUID.randomUUID().toString(),
              Instant.now(),
              event.contentId(),
              event.sourceType(),
              event.sourceUrl(),
              event.s3Key(),
              event.attempt(),
              errorMessage);
      thumbnailEventPublisher.publishFailed(failedEvent);

      try {
        kafkaTemplate.send(KafkaTopics.CONTENT_THUMBNAIL_REQUESTED_DLQ, event.contentId(), event);
      } catch (Exception ex) {
        log.error("DLQ 이벤트 발행 실패: contentId={}, s3Key={}", event.contentId(), event.s3Key(), ex);
      }

      log.error(
          "썸네일 업로드 재시도 후 실패: contentId={}, s3Key={}, sourceUrl={}",
          event.contentId(),
          event.s3Key(),
          event.sourceUrl(),
          lastFailure);
    } catch (Exception ex) {
      log.error("썸네일 요청 처리 중 예상치 못한 오류 발생: contentId={}", event.contentId(), ex);
    } finally {
      acknowledgment.acknowledge();
    }
  }
}
