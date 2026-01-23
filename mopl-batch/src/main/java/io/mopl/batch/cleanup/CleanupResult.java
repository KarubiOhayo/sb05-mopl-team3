package io.mopl.batch.cleanup;

import java.time.Instant;
import java.util.List;

public record CleanupResult(
    String runId,
    Instant from,
    Instant to,
    String s3Prefix,
    int contentCount,
    int s3KeyCount,
    List<String> s3KeySample,
    boolean dryRun,
    boolean deleteS3,
    int deletedContents,
    int deletedContentTags,
    int deletedS3Objects) {}
