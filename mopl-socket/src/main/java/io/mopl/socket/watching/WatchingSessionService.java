package io.mopl.socket.watching;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private static final String CONTENT_PREFIX = "watching:content:";
  private static final String USER_PREFIX = "watching:user:";

  private final RedisTemplate<String, String> redisTemplate;

  // userId를 받아 현재 시청중인 contentId를 반환하는 메서드
  public Optional<String> getWatchingContentId(UUID userId) {
    String contentId = redisTemplate.opsForValue().get(userKey(userId));
    return Optional.ofNullable(contentId);
  }

  // userId와 contentId를 받아 Redis에 시청 세션 정보를 저장하고 해당 컨텐츠에 몇 명의 사용자가 접속했는지 반환하는 메서드
  public long join(String contentId, UUID userId) {
    String userKey = userKey(userId);
    String previousContentId = redisTemplate.opsForValue().get(userKey);

    // 사용자는 한 번에 한 개의 시청 세션만 가질 수 있으며 다른 컨텐츠 세션에 접속했을 시 이전 세션을 삭제한다.
    if (previousContentId != null && !previousContentId.equals(contentId)) {
      redisTemplate.opsForSet().remove(contentKey(previousContentId), userId.toString());
    }

    // 레디스에 시청 세션 정보를 등록
    redisTemplate.opsForSet().add(contentKey(contentId), userId.toString());
    redisTemplate.opsForValue().set(userKey, contentId);

    return getWatcherCount(contentId);
  }

  // 시청 세션을 떠날 때 Redis에서 세션 정보를 삭제한다.
  public long leave(String contentId, UUID userId) {
    redisTemplate.opsForSet().remove(contentKey(contentId), userId.toString());
    redisTemplate.delete(userKey(userId));
    return getWatcherCount(contentId);
  }

  public long getWatcherCount(String contentId) {
    Long count = redisTemplate.opsForSet().size(contentKey(contentId));
    return count == null ? 0 : count;
  }

  private String contentKey(String contentId) {
    return CONTENT_PREFIX + contentId;
  }

  private String userKey(UUID userId) {
    return USER_PREFIX + userId;
  }
}
