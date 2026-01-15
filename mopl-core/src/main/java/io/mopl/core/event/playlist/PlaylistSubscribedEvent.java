package io.mopl.core.event.playlist;

import java.time.Instant;

public record PlaylistSubscribedEvent(
    String eventId,
    Instant occurredAt,
    String playlistId,
    String ownerId,
    String subscriberId,
    String subscriberName) {}
