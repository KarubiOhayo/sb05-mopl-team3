package io.mopl.batch.cleanup;

import java.time.Instant;

public record CleanupRequest(
    String runId, Instant from, Instant to, boolean dryRun, boolean deleteS3, int sampleSize) {}
