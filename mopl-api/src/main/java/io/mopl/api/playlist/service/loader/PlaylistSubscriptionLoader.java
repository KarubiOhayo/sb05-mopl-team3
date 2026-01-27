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
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
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

    // Redis set이 있으면 캐시로 구독 여부 확인
    try {
      Boolean keyExists = redisTemplate.hasKey(key);
      if (Boolean.TRUE.equals(keyExists)) {
        List<Object> rawResults =
            redisTemplate.executePipelined(
                new SessionCallback<List<Object>>() {
                  @Override
                  @SuppressWarnings("unchecked")
                  public <K, V> List<Object> execute(RedisOperations<K, V> operations) {
                    RedisOperations<String, String> ops =
                        (RedisOperations<String, String>) operations;
                    for (UUID playlistId : playlistIds) {
                      ops.opsForSet().isMember(key, playlistId.toString());
                    }
                    return null;
                  }
                });

        for (int i = 0; i < playlistIds.size(); i++) {
          Object raw = rawResults.get(i);
          if (Boolean.TRUE.equals(raw)) {
            result.add(playlistIds.get(i));
          }
        }
        // 키가 있었는데 결과가 비어있으면 만료/삭제 경합 가능성 → DB 조회로 대체
        if (result.isEmpty() && !playlistIds.isEmpty()) {
          log.debug("Redis 키는 존재했으나 결과가 비어있어 DB 조회로 대체 key={}", key);
        } else {
          return result;
        }
      }
    } catch (Exception e) {
      log.warn("Redis 캐시 조회 실패 key={} error={}", key, e.getMessage());
      // Redis 장애 시 DB 조회로 대체
    }

    // 캐시가 없으면 요청된 playlistIds만 DB 조회
    List<PlaylistSubscription> subs =
        subscriptionRepository.findByIdUserIdAndIdPlaylistIdIn(me, playlistIds);
    for (PlaylistSubscription sub : subs) {
      result.add(sub.getId().getPlaylistId());
    }

    return result;
  }
}
