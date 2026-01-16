package io.mopl.core.event.user;

import java.time.Instant;

public record UserRoleChangedEvent(
    String eventId, Instant occurredAt, String userId, String userName, String newRole) {}
