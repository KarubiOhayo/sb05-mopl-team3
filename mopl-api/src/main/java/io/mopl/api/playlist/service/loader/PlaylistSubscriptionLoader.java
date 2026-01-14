package io.mopl.api.playlist.service.loader;

import io.mopl.api.playlist.domain.PlaylistSubscription;
import io.mopl.api.playlist.domain.PlaylistSubscriptionRepository;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaylistSubscriptionLoader {

  private final PlaylistSubscriptionRepository subscriptionRepository;
  private final RedisTemplate<String, String> redisTemplate;

  public Set<UUID> loadSubscribedPlaylistIdsByMe(UUID me, List<UUID> playlistIds) {
    if (me == null) {
      return Set.of();
    }
    if (playlistIds == null || playlistIds.isEmpty()) {
      return Set.of();
    }

    String key = RedisKeyPrefix.PLAYLIST_SUBS_BY_USER + me;
    Set<UUID> result = new HashSet<>();

    // Redis set이 존재하면 캐시에서 구독 여부를 확인
    if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
      for (UUID playlistId : playlistIds) {
        Boolean isMember = redisTemplate.opsForSet().isMember(key, playlistId.toString());
        if (Boolean.TRUE.equals(isMember)) {
          result.add(playlistId);
        }
      }
      return result;
    }

    // 캐시가 없으면 DB에서 조회
    List<PlaylistSubscription> subs =
        subscriptionRepository.findByIdUserIdAndIdPlaylistIdIn(me, playlistIds);

    for (PlaylistSubscription sub : subs) {
      result.add(sub.getId().getPlaylistId());
    }

    // 조회된 구독 목록을 Redis set으로 저장
    if (!result.isEmpty()) {
      String[] values = result.stream().map(UUID::toString).toArray(String[]::new);
      redisTemplate.opsForSet().add(key, values);
    }

    return result;
  }
}
