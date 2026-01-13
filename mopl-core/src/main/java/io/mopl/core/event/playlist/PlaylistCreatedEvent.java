package io.mopl.core.event.playlist;

import java.time.Instant;

public record PlaylistCreatedEvent(
    String eventId, Instant occurredAt, String playlistId, String ownerId, String ownerName) {}
