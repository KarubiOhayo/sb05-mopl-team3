package io.mopl.api.playlist.service.loader;

import io.mopl.api.playlist.domain.PlaylistSubscription;
import io.mopl.api.playlist.domain.PlaylistSubscriptionRepository;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

@Slf4j
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
    try {
      if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
        @SuppressWarnings("unchecked")
        RedisSerializer<String> serializer =
            (RedisSerializer<String>) redisTemplate.getStringSerializer();
        byte[] keyBytes = serializer.serialize(key);
        List<Object> rawResults =
            redisTemplate.executePipelined(
                (RedisCallback<Object>)
                    connection -> {
                      for (UUID playlistId : playlistIds) {
                        byte[] valueBytes = serializer.serialize(playlistId.toString());
                        connection.sIsMember(keyBytes, valueBytes);
                      }
                      return null;
                    });

        for (int i = 0; i < playlistIds.size(); i++) {
          Object raw = rawResults.get(i);
          if (Boolean.TRUE.equals(raw)) {
            result.add(playlistIds.get(i));
          }
        }
        return result;
      }
    } catch (Exception e) {
      log.warn("Redis 캐시 조회 실패 key={} error={}", key, e.getMessage());
      // fallback to DB query below
    }

    // 캐시가 없으면 현재 페이지의 playlistIds만 DB에서 조회
    List<PlaylistSubscription> subs =
        subscriptionRepository.findByIdUserIdAndIdPlaylistIdIn(me, playlistIds);
    Set<UUID> subscribedIds = new HashSet<>();
    for (PlaylistSubscription sub : subs) {
      subscribedIds.add(sub.getId().getPlaylistId());
    }
    return subscribedIds;
  }
}
