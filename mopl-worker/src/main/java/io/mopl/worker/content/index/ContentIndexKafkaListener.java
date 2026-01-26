package io.mopl.worker.content.index;

import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.core.event.content.ContentIndexBatchRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.common.config.KafkaRetryProperties;
import io.mopl.worker.content.index.domain.ContentIndexQueryRepository;
import io.mopl.worker.content.index.dto.ContentIndexRow;
import io.mopl.worker.content.index.mapper.ContentIndexMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentIndexKafkaListener {

  private static final int BULK_SIZE = 10;

  private final ContentIndexQueryRepository queryRepository;
  private final ContentIndexMapper mapper;
  private final ContentElasticBulkService bulkService;
  private final KafkaRetryProperties retryProperties;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @KafkaListener(
      topics = KafkaTopics.CONTENT_INDEX_REQUESTED,
      properties =
          "spring.json.value.default.type=io.mopl.core.event.content.ContentIndexBatchRequestedEvent")
  public void handle(ContentIndexBatchRequestedEvent event, Acknowledgment acknowledgment) {
    if (event.contentIds() == null || event.contentIds().isEmpty()) {
      acknowledgment.acknowledge();
      return;
    }

    log.info("Content index event received: eventId={}", event.eventId());
    List<UUID> requestedIds = event.contentIds();
    List<ContentIndexRow> rows = queryRepository.findAllForIndexing(requestedIds);
    Set<UUID> missingIdSet = new HashSet<>(requestedIds);
    for (ContentIndexRow row : rows) {
      missingIdSet.remove(row.getId());
    }
    List<UUID> missingIds = new ArrayList<>(missingIdSet);
    if (rows.isEmpty()) {
      log.debug("No content rows found for indexing. eventId={}", event.eventId());
    }

    int maxAttempts = retryProperties.maxAttempts() == null ? 3 : retryProperties.maxAttempts();
    long backoffMs =
        retryProperties.initialBackoffMs() == null ? 1000L : retryProperties.initialBackoffMs();
    double backoffMultiplier =
        retryProperties.backoffMultiplier() == null ? 2.0 : retryProperties.backoffMultiplier();
    long maxBackoffMs =
        retryProperties.maxBackoffMs() == null ? 10000L : retryProperties.maxBackoffMs();

    Exception lastFailure = null;
    long currentBackoff = backoffMs;
    try {
      for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
          log.info("ES bulk sync attempt {}/{}", attempt, maxAttempts);
          if (!missingIds.isEmpty()) {
            bulkService.deleteByContentIds(missingIds);
          }
          if (!rows.isEmpty()) {
            bulkService.bulkUpsert(rows.stream().map(mapper::toDocument).toList(), BULK_SIZE);
          }
          lastFailure = null;
          return;
        } catch (Exception ex) {
          lastFailure = ex;
          log.warn(
              "ES bulk sync failed (attempt {}/{}): eventId={}",
              attempt,
              maxAttempts,
              event.eventId(),
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

      if (lastFailure != null) {
        try {
          kafkaTemplate
              .send(KafkaTopics.CONTENT_INDEX_REQUESTED_DLQ, event.eventId(), event)
              .get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
          log.error("Failed to publish DLQ event: eventId={}", event.eventId(), ex);
          throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        log.error("ES bulk sync failed after retries: eventId={}", event.eventId(), lastFailure);
      }
    } finally {
      acknowledgment.acknowledge();
    }
  }
}
