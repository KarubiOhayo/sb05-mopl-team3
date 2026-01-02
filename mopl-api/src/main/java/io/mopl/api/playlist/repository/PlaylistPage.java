package io.mopl.api.playlist.repository;

import java.util.List;
import java.util.UUID;

import io.mopl.api.playlist.domain.Playlist;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 커서 페이징 결과를 서비스에서 DTO로 조립하기 쉽게 묶어주는 객체
@Getter
@AllArgsConstructor
public class PlaylistPage {
	private final List<Playlist> playlists;
	private final boolean hasNext;
	private final String nextCursor;
	private final UUID nextIdAfter;

}
