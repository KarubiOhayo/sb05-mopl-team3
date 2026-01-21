package io.mopl.core.event.watching;

import java.time.Instant;

public record WatchingSessionStartedEvent(
    String eventId, Instant occurredAt, String watcherId, String watcherName, String contentId) {}
