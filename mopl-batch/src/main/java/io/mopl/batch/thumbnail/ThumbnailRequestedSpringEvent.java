package io.mopl.batch.thumbnail;

import io.mopl.core.event.thumbnail.ThumbnailSourceType;

public record ThumbnailRequestedSpringEvent(
    String contentId, ThumbnailSourceType sourceType, String sourceUrl, String s3Key) {}
