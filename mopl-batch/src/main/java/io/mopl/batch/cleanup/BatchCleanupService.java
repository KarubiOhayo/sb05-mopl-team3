package io.mopl.batch.cleanup;

import io.mopl.batch.common.BatchErrorCode;
import io.mopl.batch.content.domain.Content;
import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.batch.content.domain.ContentTagRepository;
import io.mopl.batch.s3.S3CleanupService;
import io.mopl.core.error.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BatchCleanupService {

  private static final String BENCH_PREFIX_FORMAT = "thumbnails/bench/%s/";

  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;
  private final S3CleanupService s3CleanupService;

  @Transactional
  public CleanupResult cleanup(CleanupRequest request) {
    if ((request.runId() == null || request.runId().isBlank()) && request.from() == null) {
      throw new BusinessException(BatchErrorCode.CLEANUP_INVALID_REQUEST)
          .addDetail("reason", "runId 또는 from 파라미터가 필요합니다.");
    }

    Instant from = request.from();
    Instant to = request.to() == null ? Instant.now() : request.to();
    if (from != null && from.isAfter(to)) {
      throw new BusinessException(BatchErrorCode.CLEANUP_INVALID_REQUEST)
          .addDetail("reason", "from은 to보다 이전이어야 합니다.");
    }

    String runId = request.runId();
    String prefix = null;
    List<Content> targets;
    if (runId != null && !runId.isBlank()) {
      prefix = String.format(BENCH_PREFIX_FORMAT, runId);
      targets = contentRepository.findAllByThumbnailImageKeyStartingWith(prefix);
    } else {
      targets = contentRepository.findAllByCreatedAtBetween(from, to);
    }

    List<UUID> contentIds = targets.stream().map(Content::getId).filter(id -> id != null).toList();
    List<String> s3Keys =
        targets.stream()
            .map(Content::getThumbnailImageKey)
            .filter(key -> key != null && !key.isBlank())
            .collect(Collectors.toList());

    int contentCount = contentIds.size();
    int s3KeyCount = s3Keys.size();
    List<String> s3KeySample =
        s3Keys.stream().limit(request.sampleSize()).collect(Collectors.toList());

    int deletedContents = 0;
    int deletedContentTags = 0;
    int deletedS3Objects = 0;

    if (!request.dryRun()) {
      if (!contentIds.isEmpty()) {
        deletedContentTags = contentTagRepository.deleteByContentIds(contentIds);
        contentRepository.deleteAllByIdInBatch(contentIds);
        deletedContents = contentIds.size();
      }

      if (request.deleteS3()) {
        if (prefix != null) {
          deletedS3Objects = s3CleanupService.deleteByPrefix(prefix);
        } else {
          deletedS3Objects = s3CleanupService.deleteByKeys(s3Keys);
        }
      }
    }

    return new CleanupResult(
        runId,
        from,
        to,
        prefix,
        contentCount,
        s3KeyCount,
        s3KeySample,
        request.dryRun(),
        request.deleteS3(),
        deletedContents,
        deletedContentTags,
        deletedS3Objects);
  }
}
