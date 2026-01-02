package io.mopl.api.playlist.repository;

import java.util.UUID;

public interface PlaylistQueryRepository {

	PlaylistPage findPlaylistsPage(
		String keywordLike,
		UUID ownerIdEqual,
		UUID subscriberIdEqual,
		String cursor,
		UUID idAfter,
		int limit,
		String sortDirection, // ASCENDING | DESCENDING
		String sortBy         // updatedAt | subscribeCount
	);

	long countPlaylists(String keywordLike, UUID ownerIdEqual, UUID subscriberIdEqual);

}
