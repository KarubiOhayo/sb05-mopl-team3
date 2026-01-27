package io.mopl.batch.scheduler;

import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.core.event.content.ContentAggregateUpdatedBatchEvent;
import io.mopl.core.kafka.KafkaTopics;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewAggregateRefreshScheduler {

  private static final int DEFAULT_BATCH_SIZE = 500;

  private final ContentRepository contentRepository;
  private final EntityManager entityManager;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${batch.review-aggregate.batch-size:100}")
  private int batchSize;

  @Value("${batch.review-aggregate.lookback-minutes:30}")
  private long lookbackMinutes;

  private volatile Instant lastRun = Instant.now();

  @Scheduled(fixedDelayString = "${batch.schedule.review-aggregate-interval-ms:900000}")
  public void refreshIncremental() {
    long startedAt = System.currentTimeMillis();
    Instant to = Instant.now();
    Instant from = lastRun.minus(Duration.ofMinutes(lookbackMinutes));

    try {
      List<String> contentIds = findChangedContentIds(from, to);
      if (contentIds.isEmpty()) {
        lastRun = to;
        log.info(
            "Review aggregate incremental skipped: no changes, elapsedMs={}",
            System.currentTimeMillis() - startedAt);
        return;
      }

      int totalUpdated = 0;
      int publishBatchSize = Math.max(batchSize, DEFAULT_BATCH_SIZE);
      for (List<String> batch : partition(contentIds, publishBatchSize)) {
        totalUpdated += contentRepository.refreshReviewAggregatesForContentIds(batch);
        publishAggregateUpdated(batch);
      }

      lastRun = to;
      long elapsed = System.currentTimeMillis() - startedAt;
      log.info(
          "Review aggregate incremental finished: changedContents={}, updatedRows={}, elapsedMs={}",
          contentIds.size(),
          totalUpdated,
          elapsed);
    } catch (Exception e) {
      long elapsed = System.currentTimeMillis() - startedAt;
      log.error("Review aggregate incremental failed: elapsedMs={}", elapsed, e);
    }
  }

  @Scheduled(cron = "${batch.schedule.review-aggregate-full-cron:0 0 5 * * *}")
  public void refreshFull() {
    long startedAt = System.currentTimeMillis();
    try {
      int updated = contentRepository.refreshReviewAggregates();
      publishFullAggregateUpdates();
      long elapsed = System.currentTimeMillis() - startedAt;
      log.info(
          "Review aggregate full refresh finished: updatedRows={}, elapsedMs={}", updated, elapsed);
    } catch (Exception e) {
      long elapsed = System.currentTimeMillis() - startedAt;
      log.error("Review aggregate full refresh failed: elapsedMs={}", elapsed, e);
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> findChangedContentIds(Instant from, Instant to) {
    return entityManager
        .createNativeQuery(
            "select distinct content_id from reviews where updated_at >= ?1 and updated_at < ?2")
        .setParameter(1, Timestamp.from(from))
        .setParameter(2, Timestamp.from(to))
        .getResultList();
  }

  private void publishAggregateUpdated(List<String> contentIds) {
    if (contentIds == null || contentIds.isEmpty()) {
      return;
    }
    ContentAggregateUpdatedBatchEvent event =
        new ContentAggregateUpdatedBatchEvent(
            UUID.randomUUID().toString(), Instant.now(), new ArrayList<>(contentIds));
    kafkaTemplate.send(KafkaTopics.CONTENT_AGGREGATE_UPDATED_BATCH, event);
  }

  private void publishFullAggregateUpdates() {
    int offset = 0;
    int limit = Math.max(batchSize, DEFAULT_BATCH_SIZE);
    while (true) {
      List<String> contentIds =
          mapToStringList(
              entityManager
                  .createNativeQuery("select id from contents order by id")
                  .setFirstResult(offset)
                  .setMaxResults(limit)
                  .getResultList());
      if (contentIds.isEmpty()) {
        return;
      }
      publishAggregateUpdated(contentIds);
      offset += contentIds.size();
    }
  }

  private List<String> mapToStringList(List<?> rawIds) {
    List<String> contentIds = new ArrayList<>();
    for (Object id : rawIds) {
      if (id != null) {
        contentIds.add(id.toString());
      }
    }
    return contentIds;
  }

  private List<List<String>> partition(List<String> items, int size) {
    List<List<String>> batches = new ArrayList<>();
    int total = items.size();
    int chunk = Math.max(size, 1);
    for (int i = 0; i < total; i += chunk) {
      batches.add(items.subList(i, Math.min(total, i + chunk)));
    }
    return batches;
  }
}
