package io.mopl.socket.watching;

import io.mopl.core.error.BusinessException;
import io.mopl.redis.constants.RedisKeyPrefix;
import io.mopl.socket.common.dto.CursorResponse;
import io.mopl.socket.common.dto.SortDirection;
import io.mopl.socket.common.error.SocketErrorCode;
import io.mopl.socket.content.domain.ContentRepository;
import io.mopl.socket.content.dto.ContentSummary;
import io.mopl.socket.user.dto.UserSummary;
import io.mopl.socket.watching.dto.WatchingSessionDto;
import io.mopl.socket.watching.dto.WatchingSessionSearchRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchingSessionService {

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  private final ContentRepository contentRepository;

  // userId를 받아 현재 시청중인 contentId를 반환하는 메서드
  public Optional<String> getWatchingContentId(UUID userId) {
    String contentId = redisTemplate.opsForValue().get(userKey(userId));
    return Optional.ofNullable(contentId);
  }

  // userId와 contentId를 받아 Redis에 시청 세션 정보를 저장하고 해당 컨텐츠에 몇 명의 사용자가 접속했는지 반환하는 메서드
  public long join(String contentId, UUID userId, String name, String profileImageUrl) {
    String userKey = userKey(userId);
    String previousContentId = redisTemplate.opsForValue().get(userKey);

    // 사용자는 한 번에 한 개의 시청 세션만 가질 수 있으며 다른 컨텐츠 세션에 접속했을 시 이전 세션을 삭제한다.
    if (previousContentId != null && !previousContentId.equals(contentId)) {
      leave(previousContentId, userId);
    }

    // 컨텐츠 정보 캐싱 (최초 1회 DB 조회)
    getContentInfo(UUID.fromString(contentId));

    // 레디스에 시청 세션 정보를 등록 (Sorted Set 사용, Score = 현재 시간)
    // ZSet을 사용하면 입장 시간 순으로 정렬되어 커서 페이지네이션이 가능해짐
    double score = System.currentTimeMillis();
    redisTemplate.opsForZSet().add(contentKey(contentId), userId.toString(), score);
    redisTemplate.opsForValue().set(userKey, contentId);

    // 사용자 상세 정보 저장 (이름, 프로필 이미지)
    saveUserInfo(userId, name, profileImageUrl);

    return getWatcherCount(contentId);
  }

  // 시청 세션을 떠날 때 Redis에서 세션 정보를 삭제한다.
  public long leave(String contentId, UUID userId) {
    redisTemplate.opsForZSet().remove(contentKey(contentId), userId.toString());
    redisTemplate.delete(userKey(userId));
    redisTemplate.delete(userInfoKey(userId)); // 유저 상세 정보 삭제
    return getWatcherCount(contentId);
  }

  public long getWatcherCount(String contentId) {
    Long count = redisTemplate.opsForZSet().zCard(contentKey(contentId));
    return count == null ? 0 : count;
  }

  public CursorResponse<WatchingSessionDto> findByContentId(
      UUID contentId, WatchingSessionSearchRequest request) {
    String key = contentKey(contentId.toString());

    // 1. 총 개수 조회
    Long totalCount = redisTemplate.opsForZSet().zCard(key);
    if (totalCount == null || totalCount == 0) {
      return CursorResponse.<WatchingSessionDto>builder()
          .data(Collections.emptyList())
          .hasNext(false)
          .totalCount(0)
          .build();
    }

    // 2. 조회 범위 계산 (Start, End)
    long start = 0;
    if (StringUtils.hasText(request.cursor())) {
      // 커서(이전 페이지의 마지막 userId)가 있다면, 그 유저의 랭크를 찾음
      Long rank = redisTemplate.opsForZSet().reverseRank(key, request.cursor());
      if (rank != null) {
        start = rank + 1; // 커서 다음부터 조회
      }
    }

    long end = start + request.limit() - 1;

    // 3. Redis 조회 (ZREVRANGE: 내림차순, 최신순)
    Set<String> watcherIds;
    if (request.sortDirection() == SortDirection.ASCENDING) {
      watcherIds = redisTemplate.opsForZSet().range(key, start, end);
    } else {
      watcherIds = redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    if (watcherIds == null || watcherIds.isEmpty()) {
      return CursorResponse.<WatchingSessionDto>builder()
          .data(Collections.emptyList())
          .hasNext(false)
          .totalCount(totalCount.intValue())
          .build();
    }

    // 컨텐츠 정보 미리 조회 (한 번만)
    ContentSummary contentInfo = getContentInfo(contentId);

    // 4. DTO 변환
    List<WatchingSessionDto> sessions =
        watcherIds.stream()
            .map(
                userId -> {
                  // ZSet에서 Score(입장시간) 가져오기
                  Double score = redisTemplate.opsForZSet().score(key, userId);
                  Instant joinedAt =
                      score != null ? Instant.ofEpochMilli(score.longValue()) : Instant.now();

                  // Redis에서 상세 정보 조회
                  UserSummary watcher = getUserInfo(UUID.fromString(userId));

                  return WatchingSessionDto.builder()
                      .id(UUID.fromString(userId))
                      .createdAt(joinedAt)
                      .watcher(watcher)
                      .content(contentInfo)
                      .build();
                })
            .collect(Collectors.toList());

    // 5. 다음 커서 계산
    String nextCursor = null;
    boolean hasNext = false;

    if ((start + sessions.size()) < totalCount) {
      hasNext = true;
      nextCursor = sessions.getLast().watcher().userId().toString();
    }

    return CursorResponse.<WatchingSessionDto>builder()
        .data(sessions)
        .nextCursor(nextCursor)
        .hasNext(hasNext)
        .totalCount(totalCount.intValue())
        .build();
  }

  public WatchingSessionDto findByWatcherId(UUID watcherId) {
    String contentId =
        getWatchingContentId(watcherId)
            .orElseThrow(() -> new BusinessException(SocketErrorCode.WATCHING_SESSION_NOT_FOUND));

    UserSummary watcher = getUserInfo(watcherId);
    ContentSummary content = getContentInfo(UUID.fromString(contentId));

    String key = contentKey(contentId);
    Double score = redisTemplate.opsForZSet().score(key, watcherId.toString());
    Instant joinedAt = score != null ? Instant.ofEpochMilli(score.longValue()) : Instant.now();

    return WatchingSessionDto.builder()
        .id(watcherId) // userId를 세션 ID로 사용
        .createdAt(joinedAt)
        .watcher(watcher)
        .content(content)
        .build();
  }

  private void saveUserInfo(UUID userId, String name, String profileImageUrl) {
    try {
      UserSummary userInfo =
          UserSummary.builder().userId(userId).name(name).profileImageUrl(profileImageUrl).build();
      String json = objectMapper.writeValueAsString(userInfo);
      redisTemplate.opsForValue().set(userInfoKey(userId), json);
    } catch (Exception e) {
      log.error("Redis에 사용자 정보 저장 실패", e);
    }
  }

  private UserSummary getUserInfo(UUID userId) {
    String json = redisTemplate.opsForValue().get(userInfoKey(userId));
    if (json != null) {
      try {
        return objectMapper.readValue(json, UserSummary.class);
      } catch (Exception e) {
        log.error("Redis에서 사용자 정보 파싱 실패", e);
      }
    }
    // Fallback
    return UserSummary.builder().userId(userId).name("Unknown").build();
  }

  public ContentSummary getContentInfo(UUID contentId) {
    String key = contentInfoKey(contentId);
    String json = redisTemplate.opsForValue().get(key);

    if (json != null) {
      try {
        return objectMapper.readValue(json, ContentSummary.class);
      } catch (Exception e) {
        log.error("Redis에서 컨텐츠 정보 파싱 실패", e);
      }
    }

    // Cache Miss: DB 조회
    return contentRepository
        .findById(contentId)
        .map(
            content -> {
              ContentSummary summary =
                  ContentSummary.builder()
                      .id(content.getId())
                      .title(content.getTitle())
                      .type(content.getType())
                      .thumbnailUrl(content.getThumbnailUrl())
                      .build();

              // Redis 저장 (TTL 24시간)
              try {
                String cacheValue = objectMapper.writeValueAsString(summary);
                redisTemplate.opsForValue().set(key, cacheValue, Duration.ofHours(24));
              } catch (Exception e) {
                log.error("Redis에 컨텐츠 정보 캐싱 실패", e);
              }
              return summary;
            })
        .orElse(ContentSummary.builder().id(contentId).title("알 수 없음").build());
  }

  private String contentKey(String contentId) {
    return RedisKeyPrefix.CONTENT_PREFIX + contentId;
  }

  private String userKey(UUID userId) {
    return RedisKeyPrefix.USER_PREFIX + userId;
  }

  private String userInfoKey(UUID userId) {
    return RedisKeyPrefix.USER_INFO_PREFIX + userId;
  }

  private String contentInfoKey(UUID contentId) {
    return RedisKeyPrefix.CONTENT_INFO_PREFIX + contentId;
  }
}
