package io.mopl.worker.thumbnail;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.mopl.core.event.thumbnail.ContentThumbnailCompletedEvent;
import io.mopl.core.event.thumbnail.ContentThumbnailFailedEvent;
import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.common.config.KafkaRetryProperties;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 썸네일 요청 이벤트를 처리하는 비동기 핸들러.
 *
 * <p>다운로드/업로드를 수행하고 결과 이벤트를 발행하며, 실패 시 재시도 정책을 적용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentThumbnailRequestedHandler {

  private static final ConcurrentMap<String, AtomicLong> LAST_COMPLETION_GAUGES =
      new ConcurrentHashMap<>();

  private final ThumbnailS3Uploader thumbnailS3Uploader;
  private final ThumbnailEventPublisher thumbnailEventPublisher;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaRetryProperties retryProperties;
  private final MeterRegistry meterRegistry;

  /**
   * 비동기로 썸네일 업로드를 처리한다.
   *
   * <p>재시도 후에도 실패하면 실패 이벤트와 DLQ 이벤트를 발행한다.
   *
   * @param event 요청 이벤트
   * @param acknowledgment Kafka ACK
   */
  @Async("kafkaTaskExecutor")
  public void handleAsync(ContentThumbnailRequestedEvent event, Acknowledgment acknowledgment) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String runTag = RunIdResolver.resolveFromKey(event.s3Key());
    String status = "failed";
    Counter retryCounter =
        Counter.builder("worker.thumbnail.retries").tags("run_id", runTag).register(meterRegistry);
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
        status = "skipped";
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
          status = "success";
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
            retryCounter.increment();
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
      Counter.builder("worker.thumbnail.requests")
          .tags("status", status, "run_id", runTag)
          .register(meterRegistry)
          .increment();
      sample.stop(
          Timer.builder("worker.thumbnail.handle.duration")
              .tags("status", status, "run_id", runTag)
              .register(meterRegistry));
      if (event.occurredAt() != null) {
        long durationNanos =
            Math.max(0L, java.time.Duration.between(event.occurredAt(), Instant.now()).toNanos());
        Timer.builder("worker.thumbnail.end_to_end.duration")
            .tags("status", status, "run_id", runTag)
            .register(meterRegistry)
            .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
      }
      AtomicLong gauge =
          LAST_COMPLETION_GAUGES.computeIfAbsent(
              runTag,
              key -> {
                AtomicLong value = new AtomicLong();
                meterRegistry.gauge(
                    "worker.thumbnail.last_completion.epoch_ms",
                    io.micrometer.core.instrument.Tags.of("run_id", runTag),
                    value);
                return value;
              });
      gauge.set(System.currentTimeMillis());
      acknowledgment.acknowledge();
    }
  }
}
