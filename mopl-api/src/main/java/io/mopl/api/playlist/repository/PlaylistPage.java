package io.mopl.api.playlist.repository;

import java.util.List;
import java.util.UUID;

import io.mopl.api.playlist.domain.Playlist;

// 커서 페이징 결과를 서비스에서 DTO로 조립하기 쉽게 묶어주는 객체
public class PlaylistPage {

	private final List<Playlist> playlists;
	private final boolean hasNext;
	private final String nextCursor;
	private final UUID nextIdAfter;

	public PlaylistPage(List<Playlist> playlists, boolean hasNext, String nextCursor, UUID nextIdAfter) {
		this.playlists = playlists;
		this.hasNext = hasNext;
		this.nextCursor = nextCursor;
		this.nextIdAfter = nextIdAfter;
	}

	public List<Playlist> getPlaylists() {
		return playlists;
	}

	public boolean isHasNext() {
		return hasNext;
	}

	public String getNextCursor() {
		return nextCursor;
	}

	public UUID getNextIdAfter() {
		return nextIdAfter;
	}

}
