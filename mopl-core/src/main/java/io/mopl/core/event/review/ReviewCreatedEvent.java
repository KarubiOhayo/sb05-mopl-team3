package io.mopl.core.event.review;

import java.time.Instant;

public record ReviewCreatedEvent(
    String eventId, Instant occurredAt, String reviewId, String contentId, double rating) {}
