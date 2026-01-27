package io.mopl.batch.scheduler;

import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.core.event.content.ContentAggregateUpdatedBatchEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionSnapshotScheduler {

  private static final int SCAN_COUNT = 100;
  private static final String SNAPSHOT_PREFIX = "watching:snapshot:";
  private static final Duration SNAPSHOT_TTL = Duration.ofHours(24);

  private final RedisTemplate<String, String> redisTemplate;
  private final ContentRepository contentRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${batch.watcher-es.batch-size:100}")
  private int batchSize;

  @Value("${batch.watcher-es.flush-interval-ms:5000}")
  private long flushIntervalMs;

  private final Set<String> bufferedContentIds = new LinkedHashSet<>();
  private long lastFlushAt = System.currentTimeMillis();

  @Scheduled(fixedDelayString = "${batch.schedule.watcher-snapshot-interval-ms:30000}")
  @Transactional
  public void syncWatcherCounts() {
    long startedAt = System.currentTimeMillis();

    int updated = 0;
    int cleared = 0;
    int invalidKeys = 0;

    log.info(
        "Watching session snapshot sync started: cleared={}, updated={}, invalidKeys={}, startedAt={}",
        cleared,
        updated,
        invalidKeys,
        startedAt);

    Set<String> contentKeys = scanContentKeys();
    java.util.Set<String> currentContentIds = new java.util.HashSet<>();

    for (String key : contentKeys) {
      String contentId = key.substring(RedisKeyPrefix.CONTENT_PREFIX.length());
      UUID contentUuid;
      try {
        contentUuid = UUID.fromString(contentId);
      } catch (IllegalArgumentException e) {
        invalidKeys++;
        continue;
      }
      Long count = redisTemplate.opsForZSet().zCard(key);
      if (count == null) {
        continue;
      }

      String snapshotKey = SNAPSHOT_PREFIX + contentId;
      Long prevCount = parseLong(redisTemplate.opsForValue().get(snapshotKey));
      if (prevCount == null || prevCount.longValue() != count) {
        contentRepository.updateWatcherCount(contentUuid, count);
        redisTemplate.opsForValue().set(snapshotKey, Long.toString(count), SNAPSHOT_TTL);
        bufferContentId(contentId);
        updated++;
      }
      currentContentIds.add(contentId);
    }

    for (String snapshotKey : scanSnapshotKeys()) {
      String contentId = snapshotKey.substring(SNAPSHOT_PREFIX.length());
      if (currentContentIds.contains(contentId)) {
        continue;
      }
      UUID contentUuid;
      try {
        contentUuid = UUID.fromString(contentId);
      } catch (IllegalArgumentException e) {
        invalidKeys++;
        continue;
      }
      contentRepository.updateWatcherCount(contentUuid, 0);
      redisTemplate.delete(snapshotKey);
      bufferContentId(contentId);
      cleared++;
    }

    flushIfDue();

    long elapsed = System.currentTimeMillis() - startedAt;
    log.info(
        "Watching session snapshot sync finished: cleared={}, updated={}, invalidKeys={}, elapsedMs={}",
        cleared,
        updated,
        invalidKeys,
        elapsed);
  }

  @Scheduled(fixedDelayString = "${batch.watcher-es.flush-interval-ms:15000}")
  public void flushBufferedUpdates() {
    flushIfDue();
  }

  private Set<String> scanContentKeys() {
    RedisCallback<Set<String>> callback =
        connection -> {
          ScanOptions options =
              ScanOptions.scanOptions()
                  .match(RedisKeyPrefix.CONTENT_PREFIX + "*")
                  .count(SCAN_COUNT)
                  .build();
          StringRedisSerializer serializer = new StringRedisSerializer();
          Set<String> keys = new HashSet<>();
          try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
              String key = serializer.deserialize(cursor.next());
              if (key != null) {
                keys.add(key);
              }
            }
          } catch (RuntimeException e) {
            log.warn("Watching session key scan failed", e);
          }
          return keys;
        };

    Set<String> result = redisTemplate.execute(callback);
    return result == null ? Collections.emptySet() : result;
  }

  private Set<String> scanSnapshotKeys() {
    RedisCallback<Set<String>> callback =
        connection -> {
          ScanOptions options =
              ScanOptions.scanOptions().match(SNAPSHOT_PREFIX + "*").count(SCAN_COUNT).build();
          StringRedisSerializer serializer = new StringRedisSerializer();
          Set<String> keys = new HashSet<>();
          try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
              String key = serializer.deserialize(cursor.next());
              if (key != null) {
                keys.add(key);
              }
            }
          } catch (RuntimeException e) {
            log.warn("Watching snapshot key scan failed", e);
          }
          return keys;
        };

    Set<String> result = redisTemplate.execute(callback);
    return result == null ? Collections.emptySet() : result;
  }

  private Long parseLong(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return Long.valueOf(value.toString());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private void bufferContentId(String contentId) {
    bufferedContentIds.add(contentId);
    if (bufferedContentIds.size() >= batchSize) {
      flushBuffer();
    }
  }

  private void flushIfDue() {
    if (bufferedContentIds.isEmpty()) {
      return;
    }
    long now = System.currentTimeMillis();
    if ((now - lastFlushAt) >= flushIntervalMs) {
      flushBuffer();
    }
  }

  private void flushBuffer() {
    if (bufferedContentIds.isEmpty()) {
      return;
    }
    List<String> contentIds = new ArrayList<>(bufferedContentIds);
    bufferedContentIds.clear();
    lastFlushAt = System.currentTimeMillis();

    ContentAggregateUpdatedBatchEvent event =
        new ContentAggregateUpdatedBatchEvent(
            UUID.randomUUID().toString(), Instant.now(), contentIds);
    kafkaTemplate.send(KafkaTopics.CONTENT_AGGREGATE_UPDATED_BATCH, event);
  }
}
