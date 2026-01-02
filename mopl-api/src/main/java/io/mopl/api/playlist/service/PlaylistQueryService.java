package io.mopl.api.playlist.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.mopl.api.content.dto.ContentSummary;
import io.mopl.api.playlist.domain.Playlist;
import io.mopl.api.playlist.dto.CursorResponsePlaylistDto;
import io.mopl.api.playlist.dto.PlaylistDto;
import io.mopl.api.playlist.dto.PlaylistSearchRequest;
import io.mopl.api.playlist.repository.PlaylistPage;
import io.mopl.api.playlist.repository.PlaylistQueryRepository;
import io.mopl.api.playlist.service.loader.PlaylistContentLoader;
import io.mopl.api.playlist.service.loader.PlaylistOwnerLoader;
import io.mopl.api.playlist.service.loader.PlaylistSubscriptionLoader;
import io.mopl.api.user.dto.UserSummary;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistQueryService {

	private final PlaylistQueryRepository playlistQueryRepository;
	private final PlaylistOwnerLoader playlistOwnerLoader;
	private final PlaylistSubscriptionLoader playlistSubscriptionLoader;
	private final PlaylistContentLoader playlistContentLoader;

	public CursorResponsePlaylistDto findPlaylists(PlaylistSearchRequest request, UUID me) {

		// 1) 파라미터 기본값/안전장치
		Integer limitObj = request.getLimit();
		int limit = (limitObj == null) ? 20 : limitObj.intValue();
		if (limit < 1) {
			limit = 1;
		}
		if (limit > 100) {
			limit = 100;
		}
		String sortBy = request.getSortByOrDefault();
		String sortDirection = request.getSortDirectionOrDefault();

		// 2) QueryDSL로 "플레이리스트 목록" 조회
		//    - 여기서 contents/owner/subscribed를 join으로 다 끌고오면 중복/폭발 쉬움
		//    - 그래서 "플레이리스트 기본 목록"만 먼저 가져오고
		//    - 나머지는 로더로 IN 조회(1~3번 추가쿼리)로 붙임
		PlaylistPage page = playlistQueryRepository.findPlaylistsPage(
			request.getKeywordLike(),
			request.getOwnerIdEqual(),
			request.getSubscriberIdEqual(),
			request.getCursor(),
			request.getIdAfter(),
			limit,
			sortDirection,
			sortBy
		);

		long totalCount = playlistQueryRepository.countPlaylists(
			request.getKeywordLike(),
			request.getOwnerIdEqual(),
			request.getSubscriberIdEqual()
		);

		List<Playlist> playlists = page.getPlaylists();

		// 3) 로더용 ID 수집
		Set<UUID> ownerIds = new HashSet<UUID>();
		List<UUID> playlistIds = new ArrayList<UUID>();

		for (Playlist playlist : playlists) {
			ownerIds.add(playlist.getOwnerId());
			playlistIds.add(playlist.getId());
		}

		// 4) 로더로 "한 번에" 부가정보 로딩
		Map<UUID, UserSummary> ownerMap = playlistOwnerLoader.loadOwners(ownerIds);
		Set<UUID> subscribedPlaylistIds = playlistSubscriptionLoader.loadSubscribedPlaylistIdsByMe(me, playlistIds);

		// Map<UUID, List<ContentSummary>> contentsMap = playlistContentLoader.loadContentsByPlaylist(playlistIds);
		Map<UUID, List<ContentSummary>> contentsMap = playlistContentLoader.loadContentsByPlaylistIds(playlistIds);

		// 5) PlaylistDto 조립
		List<PlaylistDto> data = new ArrayList<PlaylistDto>();

		for (Playlist playlist : playlists) {
			UUID playlistId = playlist.getId();

			UserSummary owner = ownerMap.get(playlist.getOwnerId());
			boolean subscribedByMe = subscribedPlaylistIds.contains(playlistId);

			List<ContentSummary> contents = contentsMap.get(playlistId);
			if (contents == null) {
				contents = List.of();
			}

			PlaylistDto dto = PlaylistDto.builder()
				.id(playlistId)
				.owner(owner)
				.title(playlist.getTitle())
				.description(playlist.getDescription())
				.updatedAt(playlist.getUpdatedAt())
				.subscriberCount(playlist.getSubscriberCount())
				.subscribedByMe(subscribedByMe)
				.contents(contents)
				.build();

			data.add(dto);
		}

		// 6) CursorResponse 조립
		return CursorResponsePlaylistDto.builder()
			.data(data)
			.nextCursor(page.getNextCursor())
			.nextIdAfter(page.getNextIdAfter())
			.hasNext(page.isHasNext())
			.totalCount(totalCount)
			.sortBy(sortBy)
			.sortDirection(sortDirection)
			.build();
	}
}
