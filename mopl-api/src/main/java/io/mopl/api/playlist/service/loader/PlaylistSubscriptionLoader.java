package io.mopl.api.playlist.service.loader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.mopl.api.playlist.domain.PlaylistSubscription;
import io.mopl.api.playlist.repository.PlaylistSubscriptionRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlaylistSubscriptionLoader {

	private final PlaylistSubscriptionRepository subscriptionRepository;

	/**
	 * @param me 사용자 ID
	 * @param playlistIds 현재 조회된 플레이리스트 ID 목록
	 * @return 내가 구독한 playlistId 집합
	 */
	public Set<UUID> loadSubscribedPlaylistIdsByMe(UUID me, List<UUID> playlistIds) {

		// 1. 로그인 안했을 때는 무조건 false
		if (me == null){
			return Set.of();
		}

		// 조회 대상이 없으면 쿼리X
		if (playlistIds == null || playlistIds.isEmpty()){
			return Set.of();
		}

		// DB에 1번만 물어봐서 구독 레코드 갖고오기
		List<PlaylistSubscription> subs = subscriptionRepository.findByIdUserIdAndIdPlaylistIdIn(me, playlistIds);

		// 서비스에서 contains로 빠르게 판단할 수 있게 Set으로 변환
		Set<UUID> subscribedPlaylistIds = new HashSet<UUID>();
		for (PlaylistSubscription sub : subs) {
			subscribedPlaylistIds.add(sub.getId().getPlaylistId());
		}
		return subscribedPlaylistIds;
	}

}
