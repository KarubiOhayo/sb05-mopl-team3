package io.mopl.core.event.follow;

import java.time.Instant;

public record UserFollowedEvent(
    String eventId,
    Instant occurredAt,
    String followerId,
    String followerName, // worker가 DB 조회 안 하도록 포함 추천
    String followeeId) {}
