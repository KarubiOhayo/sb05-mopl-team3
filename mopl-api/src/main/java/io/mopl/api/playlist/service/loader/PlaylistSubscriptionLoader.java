package io.mopl.api.playlist.service.loader;

import io.mopl.api.playlist.domain.PlaylistSubscription;
import io.mopl.api.playlist.domain.PlaylistSubscriptionRepository;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
        for (UUID playlistId : playlistIds) {
          Boolean isMember = redisTemplate.opsForSet().isMember(key, playlistId.toString());
          if (Boolean.TRUE.equals(isMember)) {
            result.add(playlistId);
          }
        }
        return result;
      }
    } catch (Exception e) {
      log.warn("Redis 캐시 조회 실패 key={} error={}", key, e.getMessage());
      // fallback to DB query below
    }

    // 캐시가 없으면 사용자의 전체 구독 목록을 DB에서 조회
    List<PlaylistSubscription> allSubs = subscriptionRepository.findByIdUserId(me);
    Set<UUID> allSubscribedIds = new HashSet<>();
    for (PlaylistSubscription sub : allSubs) {
      allSubscribedIds.add(sub.getId().getPlaylistId());
    }

    // 전체 구독 목록을 Redis에 저장
    if (!allSubscribedIds.isEmpty()) {
      try {
        String[] values = allSubscribedIds.stream().map(UUID::toString).toArray(String[]::new);
        redisTemplate.opsForSet().add(key, values);
        redisTemplate.expire(key, Duration.ofHours(6));
      } catch (Exception e) {
        log.warn("Redis 캐시 저장 실패 key={} error={}", key, e.getMessage());
      }
    }

    // 요청된 playlistIds 중 구독된 것만 반환
    for (UUID playlistId : playlistIds) {
      if (allSubscribedIds.contains(playlistId)) {
        result.add(playlistId);
      }
    }

    return result;
  }
}
