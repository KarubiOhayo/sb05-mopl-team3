package io.mopl.api.playlist.service.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


import org.springframework.stereotype.Component;

import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserSummary;
import lombok.RequiredArgsConstructor;

/**
 * 플레이리스트 목록 N개에 대해 ownerId가 N개 있을 때,
 * users를 한 번에(IN) 조회해서 Map<UUID, UserSummary>로 만들어주는 역할.
 */
@Component
@RequiredArgsConstructor
public class PlaylistOwnerLoader {

	private final UserRepository userRepository;

	/**
	 * ownerIds로 User를 한 번에 조회해서 UserSummary로 변환 후 Map으로 반환
	 *
	 * @param ownerIds 플레이리스트 목록에서 뽑아온 ownerId 집합
	 * @return key=ownerId, value=UserSummary
	 */
	public Map<UUID, UserSummary> loadOwners(Set<UUID> ownerIds) {
		// IN절 대상이 비어있으면 쿼리를 날릴 필요 없음
		if (ownerIds == null || ownerIds.isEmpty()) {
			return Map.of();
		}
		// 결과를 Map으로 만들어 두면 서비스에서 ownerId로 O(1) 조회 가능
		Map<UUID, UserSummary> result = new HashMap<>();

		// JPA 기본 제공 findAllById는 내부적으로 IN 쿼리로 조회.
		List<User> users = userRepository.findAllById(ownerIds);

		for (User user : users) {
			UserSummary summary = UserSummary.builder()
				.userId(user.getId())
				.name(user.getName())
				.profileImageUrl(user.getProfileImageUrl())
				.build();

			result.put(user.getId(), summary);
		}

		return result;
	}
}
