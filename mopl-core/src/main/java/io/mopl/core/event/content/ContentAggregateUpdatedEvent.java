package io.mopl.core.event.content;

import java.time.Instant;

public record ContentAggregateUpdatedEvent(String eventId, Instant occurredAt, String contentId) {}
