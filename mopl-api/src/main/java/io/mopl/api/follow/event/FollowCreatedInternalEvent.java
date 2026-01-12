package io.mopl.api.follow.event;

import java.util.UUID;

public record FollowCreatedInternalEvent(UUID followerId, UUID followeeId, String followerName) {}
