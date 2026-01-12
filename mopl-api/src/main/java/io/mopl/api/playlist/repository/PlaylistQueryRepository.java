package io.mopl.api.playlist.repository;

import io.mopl.api.playlist.dto.PlaylistPage;
import io.mopl.api.playlist.dto.PlaylistSearchRequest;

public interface PlaylistQueryRepository {

  PlaylistPage findPlaylistsPage(PlaylistSearchRequest playlistSearchRequest);

  long countPlaylists(PlaylistSearchRequest playlistSearchRequest);
}
