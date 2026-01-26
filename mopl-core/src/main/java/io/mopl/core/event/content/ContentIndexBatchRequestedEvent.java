package io.mopl.core.event.content;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContentIndexBatchRequestedEvent(
    String eventId, Instant createdAt, List<UUID> contentIds, int attempt) {}
