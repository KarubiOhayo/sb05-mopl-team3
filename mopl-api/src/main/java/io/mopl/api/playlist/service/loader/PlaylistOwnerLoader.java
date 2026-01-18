package io.mopl.api.playlist.service.loader;

import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

// 플레이리스트 목록 N개에 대해 ownerId가 N개 있을 때, users를 한 번에(IN) 조회해서 Map<UUID, UserSummary>로 만들어주는 역할.
@Component
@RequiredArgsConstructor
public class PlaylistOwnerLoader {

  private final UserRepository userRepository;
  private final RedisTemplate<String, Object> redisTemplateForObject;

  public Map<UUID, UserSummary> loadOwners(Set<UUID> ownerIds) {

    // IN절 대상이 비어있으면 쿼리를 날릴 필요 없음
    if (ownerIds == null || ownerIds.isEmpty()) {
      return Map.of();
    }
    // 결과를 Map 으로 만들어 두면 서비스에서 ownerId로 O(1) 조회 가능
    Map<UUID, UserSummary> result = new HashMap<>();

    // Redis multiGet 결과 순서 매핑을 위해 고정 리스트로 변환
    List<UUID> ownerIdList = new ArrayList<>(ownerIds);

    // Redis 키 리스트 생성
    List<String> keys = ownerIdList.stream().map(id -> RedisKeyPrefix.USER_SUMMARY + id).toList();

    // Redis 일괄 조회
    List<Object> cached = redisTemplateForObject.opsForValue().multiGet(keys);

    // 캐시 hit/miss 분리
    List<UUID> missIds = new ArrayList<>();
    if (cached != null) {
      for (int i = 0; i < ownerIdList.size(); i++) {
        Object value = cached.get(i);
        if (value instanceof UserSummary summary) {
          result.put(ownerIdList.get(i), summary);
        } else {
          missIds.add(ownerIdList.get(i));
        }
      }
    } else {
      missIds.addAll(ownerIdList);
    }

    // 캐시 미스만 DB 조회 후 Redis 저장
    if (!missIds.isEmpty()) {
      List<User> users = userRepository.findAllById(missIds);
      for (User user : users) {
        UserSummary summary =
            UserSummary.builder()
                .userId(user.getId())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageKey())
                .build();

        result.put(user.getId(), summary);

        // Redis 오류 무시
        try {
          redisTemplateForObject
              .opsForValue()
              .set(RedisKeyPrefix.USER_SUMMARY + user.getId(), summary, Duration.ofHours(6));
        } catch (Exception ignored) {
          // 캐시 실패는 무시하고 진행
        }
      }
    }
    return result;
  }
}
