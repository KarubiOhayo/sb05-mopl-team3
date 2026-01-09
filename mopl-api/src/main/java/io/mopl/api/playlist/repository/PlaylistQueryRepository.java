package io.mopl.api.playlist.repository;

import io.mopl.api.playlist.dto.PlaylistPage;
import io.mopl.api.playlist.dto.PlaylistSearchRequest;
import java.util.UUID;

public interface PlaylistQueryRepository {

  PlaylistPage findPlaylistsPage(PlaylistSearchRequest playlistSearchRequest);

  long countPlaylists(String keywordLike, UUID ownerIdEqual, UUID subscriberIdEqual);
}
