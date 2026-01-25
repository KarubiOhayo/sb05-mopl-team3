package io.mopl.api.review.service.cache;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.review.dto.ReviewCursorRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewCacheService {

  private static final int CACHEABLE_LIMIT = 20;
  private static final Duration FIRST_PAGE_TTL = Duration.ofMinutes(5);

  private final RedisTemplate<String, Object> redisTemplateForObject;

  public CursorResponse<ReviewDto> getFirstPage(UUID contentId, ReviewCursorRequest request) {
    String key = buildKeyIfCacheable(contentId, request);
    if (key == null) {
      return null;
    }

    try {
      Object cached = redisTemplateForObject.opsForValue().get(key);
      if (cached instanceof ReviewPageCache cache) {
        return cache.toCursorResponse();
      }
      if (cached != null) {
        redisTemplateForObject.delete(key);
      }
    } catch (Exception e) {
      log.warn("Redis 캐시 조회 실패 key={} error={}", key, e.getMessage());
    }
    return null;
  }

  public void cacheFirstPage(
      UUID contentId, ReviewCursorRequest request, CursorResponse<ReviewDto> response) {
    String key = buildKeyIfCacheable(contentId, request);
    if (key == null || response == null) {
      return;
    }

    try {
      ReviewPageCache cache = ReviewPageCache.from(response);
      if (cache == null) {
        return;
      }
      redisTemplateForObject.opsForValue().set(key, cache, FIRST_PAGE_TTL);
    } catch (Exception e) {
      log.warn("Redis 캐시 저장 실패 key={} error={}", key, e.getMessage());
    }
  }

  public void evictFirstPage(UUID contentId) {
    if (contentId == null) {
      return;
    }

    for (String sortBy : new String[] {"createdAt", "rating"}) {
      for (String sortDirection : new String[] {"DESCENDING", "ASCENDING"}) {
        String key = buildKey(contentId, sortBy, sortDirection, CACHEABLE_LIMIT);
        try {
          redisTemplateForObject.delete(key);
        } catch (Exception e) {
          log.warn("Redis 캐시 삭제 실패 key={} error={}", key, e.getMessage());
        }
      }
    }
  }

  private String buildKeyIfCacheable(UUID contentId, ReviewCursorRequest request) {
    if (contentId == null || request == null) {
      return null;
    }
    String cursor = request.getCursor();
    if (cursor != null && !cursor.isBlank()) {
      return null;
    }
    if (request.getIdAfter() != null) {
      return null;
    }
    int limit = request.getLimitOrDefault();
    if (limit != CACHEABLE_LIMIT) {
      return null;
    }

    String sortBy = normalizeSortBy(request.getSortBy());
    String sortDirection = normalizeSortDirection(request.getSortDirection());
    if (sortBy == null || sortDirection == null) {
      return null;
    }

    return buildKey(contentId, sortBy, sortDirection, limit);
  }

  private String buildKey(UUID contentId, String sortBy, String sortDirection, int limit) {
    return RedisKeyPrefix.REVIEW_LIST
        + contentId
        + ":sort:"
        + sortBy
        + ":dir:"
        + sortDirection
        + ":limit:"
        + limit;
  }

  private String normalizeSortBy(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return "createdAt";
    }
    if ("rating".equalsIgnoreCase(sortBy)) {
      return "rating";
    }
    if ("createdAt".equalsIgnoreCase(sortBy)) {
      return "createdAt";
    }
    return null;
  }

  private String normalizeSortDirection(String sortDirection) {
    if (sortDirection == null || sortDirection.isBlank()) {
      return "DESCENDING";
    }
    if ("ASCENDING".equalsIgnoreCase(sortDirection)) {
      return "ASCENDING";
    }
    if ("DESCENDING".equalsIgnoreCase(sortDirection)) {
      return "DESCENDING";
    }
    return null;
  }
}
