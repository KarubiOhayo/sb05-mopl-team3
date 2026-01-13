package io.mopl.core.event.playlist;

import java.time.Instant;

public record PlaylistContentAddedEvent(
    String eventId, Instant occurredAt, String playlistId, String ownerId, String contentId) {}
