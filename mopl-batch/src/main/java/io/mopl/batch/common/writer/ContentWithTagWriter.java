package io.mopl.batch.common.writer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.mopl.batch.common.event.ContentIndexBatchSpringEvent;
import io.mopl.batch.content.domain.Content;
import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.batch.content.domain.ContentTag;
import io.mopl.batch.content.domain.ContentTagId;
import io.mopl.batch.content.domain.ContentTagRepository;
import io.mopl.batch.content.domain.Tag;
import io.mopl.batch.content.domain.TagRepository;
import io.mopl.batch.metrics.BatchMetricsSupport;
import io.mopl.batch.thumbnail.ThumbnailRequestedSpringEvent;
import io.mopl.core.event.thumbnail.ThumbnailSourceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수집된 콘텐츠를 저장하고 태그를 연결한 뒤 썸네일 요청 이벤트를 발행하는 Writer.
 *
 * <p>처리 흐름:
 *
 * <ol>
 *   <li>콘텐츠 ID 생성 및 썸네일 S3 키 설정
 *   <li>콘텐츠 저장
 *   <li>태그 저장/연결
 *   <li>썸네일 요청 이벤트 발행
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentWithTagWriter implements ItemWriter<Content> {

  private final ContentRepository contentRepository;
  private final TagRepository tagRepository;
  private final ContentTagRepository contentTagRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final MeterRegistry meterRegistry;

  /**
   * 청크 단위로 콘텐츠 저장과 태그 연결을 수행한다.
   *
   * <p>중복 여부는 Processor 단계에서 이미 필터링되었다는 전제다.
   *
   * @param chunk 저장할 콘텐츠 목록
   */
  @Override
  @Transactional
  public void write(Chunk<? extends Content> chunk) {
    String jobName = BatchMetricsSupport.resolveJobName();
    String stepName = BatchMetricsSupport.resolveStepName();
    String runId = BatchMetricsSupport.resolveJobParameter("runId");
    String runTag = runId == null || runId.isBlank() ? "none" : runId;
    Timer.Sample sample = Timer.start(meterRegistry);
    DistributionSummary.builder("batch.content.write.items")
        .tags("job", jobName, "step", stepName, "run_id", runTag)
        .register(meterRegistry)
        .record(chunk.size());
    Counter thumbnailEventCounter =
        Counter.builder("batch.thumbnail.events.published")
            .tags("job", jobName, "step", stepName, "run_id", runTag)
            .register(meterRegistry);

    try {
      List<UUID> indexedIds = new ArrayList<>();
      for (Content content : chunk) {
        content.generateId();

        String sourceUrl = content.getSourceThumbnailUrl();
        ThumbnailSourceType sourceType =
            content.getThumbnailSourceType() != null
                ? content.getThumbnailSourceType()
                : ThumbnailSourceType.UNKNOWN;
        String s3Key =
            buildThumbnailS3Key(
                resolveThumbnailPrefix(),
                content.getType().name(),
                content.getId().toString(),
                sourceUrl);
        content.setThumbnailImageKey(s3Key);

        // 1. 저장 (Processor에서 중복은 이미 걸러짐)
        Content savedContent = contentRepository.save(content);
        indexedIds.add(savedContent.getId());

        // 2. Tag 저장 및 연결
        if (content.getTags() != null) {
          for (String tagName : content.getTags()) {
            if (tagName == null || tagName.isBlank()) {
              continue;
            }

            // 태그가 없으면 생성, 있으면 조회
            Tag tag =
                tagRepository
                    .findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

            // ContentTag 연결
            ContentTag contentTag =
                ContentTag.builder()
                    .id(new ContentTagId(savedContent.getId(), tag.getId()))
                    .build();

            contentTagRepository.save(contentTag);
          }
        }

        if (!sourceUrl.isBlank()) {
          eventPublisher.publishEvent(
              new ThumbnailRequestedSpringEvent(
                  savedContent.getId().toString(), sourceType, sourceUrl, s3Key));
          thumbnailEventCounter.increment();
        }

        if (!indexedIds.isEmpty()) {
          eventPublisher.publishEvent(new ContentIndexBatchSpringEvent(indexedIds));
        }
      }
    } finally {
      sample.stop(
          Timer.builder("batch.content.write.duration")
              .tags("job", jobName, "step", stepName, "run_id", runTag)
              .register(meterRegistry));
    }
  }

  private static String buildThumbnailS3Key(
      String prefix, String contentType, String contentId, String sourceUrl) {
    String extension = extractExtension(sourceUrl);
    return String.format(
        "%s%s/%s.%s",
        normalizePrefix(prefix), contentType.toLowerCase(Locale.ROOT), contentId, extension);
  }

  private static String resolveThumbnailPrefix() {
    String runId = BatchMetricsSupport.resolveJobParameter("runId");
    if (runId == null || runId.isBlank()) {
      return "thumbnails/";
    }
    return String.format("thumbnails/bench/%s/", runId);
  }

  private static String normalizePrefix(String prefix) {
    if (prefix == null || prefix.isBlank()) {
      return "thumbnails/";
    }
    return prefix.endsWith("/") ? prefix : prefix + "/";
  }

  private static String extractExtension(String sourceUrl) {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      return "jpg";
    }

    int queryIndex = sourceUrl.indexOf('?');
    String sanitized = queryIndex >= 0 ? sourceUrl.substring(0, queryIndex) : sourceUrl;
    sanitized = stripTrailingMediumSegment(sanitized);
    int dotIndex = sanitized.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == sanitized.length() - 1) {
      return "jpg";
    }
    String extension = sanitized.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    return extension.isBlank() ? "jpg" : extension;
  }

  private static String stripTrailingMediumSegment(String url) {
    if (url == null || url.isBlank()) {
      return url;
    }
    String suffix = "/medium";
    if (url.length() >= suffix.length()
        && url.substring(url.length() - suffix.length()).equalsIgnoreCase(suffix)) {
      return url.substring(0, url.length() - suffix.length());
    }
    return url;
  }
}
