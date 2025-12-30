package io.mopl.core.event.thumbnail;

import java.time.Instant;

public record ContentThumbnailCompletedEvent(
    String eventId,
    Instant occurredAt,
    String contentId,
    ThumbnailSourceType sourceType,
    String sourceUrl,
    String s3Key,
    int attempt) {}
