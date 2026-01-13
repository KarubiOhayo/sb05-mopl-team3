package io.mopl.api.playlist.event;

import java.util.UUID;

public record PlaylistCreatedInternalEvent(UUID playlistId, UUID ownerId, String ownerName) {}
