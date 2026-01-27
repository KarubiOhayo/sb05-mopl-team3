package io.mopl.core.event.content;

import java.time.Instant;
import java.util.List;

public record ContentAggregateUpdatedBatchEvent(
    String eventId, Instant occurredAt, List<String> contentIds) {}
