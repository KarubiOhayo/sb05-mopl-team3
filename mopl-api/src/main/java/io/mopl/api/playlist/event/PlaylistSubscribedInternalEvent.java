package io.mopl.api.playlist.event;

import java.util.UUID;

public record PlaylistSubscribedInternalEvent(
    UUID playlistId, UUID ownerId, UUID subscriberId, String subscriberName) {}
