package io.mopl.api.playlist.event;

import java.util.UUID;

public record PlaylistContentAddedInternalEvent(UUID playlistId, UUID ownerId, UUID contentId) {}
