package io.mopl.core.event.review;

import java.time.Instant;

public record ReviewUpdatedEvent(
    String eventId,
    Instant occurredAt,
    String reviewId,
    String contentId,
    double beforeRating,
    double afterRating) {}
