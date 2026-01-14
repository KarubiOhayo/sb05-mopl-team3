package io.mopl.api.playlist.service.loader;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.mopl.api.content.domain.QContent;
import io.mopl.api.content.domain.QContentTag;
import io.mopl.api.content.domain.QTag;
import io.mopl.api.content.dto.ContentSummary;
import io.mopl.api.playlist.domain.QPlaylistContent;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistContentLoader {

  private final JPAQueryFactory queryFactory;
  private final RedisTemplate<String, Object> redisTemplateForObject;

  public Map<UUID, List<ContentSummary>> loadContentsByPlaylistIds(List<UUID> playlistIds) {
    if (playlistIds == null || playlistIds.isEmpty()) {
      return Map.of();
    }

    // Redis 캐시 조회 (playlistId -> List<ContentSummary>)
    List<UUID> playlistIdList = new ArrayList<>(playlistIds);
    List<String> keys =
        playlistIdList.stream().map(id -> RedisKeyPrefix.PLAYLIST_CONTENTS + id).toList();

    // 개별 조회로 역직렬화 문제 키만 제거
    List<Object> cached = new ArrayList<>(keys.size());
    for (String key : keys) {
      try {
        cached.add(redisTemplateForObject.opsForValue().get(key));
      } catch (Exception e) {
        log.warn("Redis 캐시 조회 실패 key={} error={}", key, e.getMessage());
        redisTemplateForObject.delete(key);
        cached.add(null);
      }
    }

    // 캐시 hit/miss 분류
    Map<UUID, List<ContentSummary>> result = new HashMap<>();
    List<UUID> missIds = new ArrayList<>();

    for (int i = 0; i < playlistIdList.size(); i++) {
      Object value = cached.get(i);
      if (value instanceof List<?> list) {
        if (list.isEmpty()) {
          result.put(playlistIdList.get(i), List.of());
        } else if (list.get(0) instanceof ContentSummary) {
          @SuppressWarnings("unchecked")
          List<ContentSummary> summaries = (List<ContentSummary>) list;
          result.put(playlistIdList.get(i), summaries);
        } else {
          missIds.add(playlistIdList.get(i));
        }
      } else {
        missIds.add(playlistIdList.get(i));
      }
    }

    // 전부 캐시 hit이면 바로 반환
    if (missIds.isEmpty()) {
      return result;
    }

    QPlaylistContent pc = QPlaylistContent.playlistContent;
    QContent c = QContent.content;

    // 캐시 미스 대상만 DB에서 조회
    List<Tuple> rows =
        queryFactory
            .select(
                pc.id.playlistId,
                pc.id.contentId,
                c.type,
                c.title,
                c.description,
                c.thumbnailUrl,
                c.averageRating,
                c.reviewCount)
            .from(pc)
            .join(c)
            .on(pc.id.contentId.eq(c.id))
            .where(pc.id.playlistId.in(missIds))
            .orderBy(pc.id.playlistId.asc(), pc.addedAt.desc())
            .fetch();

    // DB 결과를 재구성하기 위한 중간 자료구조
    Map<UUID, List<UUID>> contentIdsByPlaylistId = new HashMap<>();
    Map<UUID, ContentBase> baseByContentId = new HashMap<>();
    Set<UUID> allContentIds = new HashSet<>();

    for (Tuple row : rows) {
      UUID playlistId = row.get(pc.id.playlistId);
      UUID contentId = row.get(pc.id.contentId);

      contentIdsByPlaylistId.computeIfAbsent(playlistId, k -> new ArrayList<>()).add(contentId);
      allContentIds.add(contentId);

      if (!baseByContentId.containsKey(contentId)) {
        Double avg = row.get(c.averageRating);
        double avgValue = avg != null ? avg.doubleValue() : 0.0d;
        Integer reviewCount = row.get(c.reviewCount);

        ContentBase base =
            new ContentBase(
                contentId,
                row.get(c.type),
                row.get(c.title),
                row.get(c.description),
                row.get(c.thumbnailUrl),
                avgValue,
                reviewCount != null ? reviewCount.intValue() : 0);
        baseByContentId.put(contentId, base);
      }
    }

    if (allContentIds.isEmpty()) {
      for (UUID playlistId : missIds) {
        result.putIfAbsent(playlistId, List.of());
      }
      cacheContents(result, missIds);
      return result;
    }

    QContentTag ct = QContentTag.contentTag;
    QTag t = QTag.tag;

    // 태그 조회
    List<Tuple> tagRows =
        queryFactory
            .select(ct.id.contentId, t.name)
            .from(ct)
            .join(t)
            .on(ct.id.tagId.eq(t.id))
            .where(ct.id.contentId.in(allContentIds))
            .fetch();

    Map<UUID, List<String>> tagsByContentId = new HashMap<>();
    for (Tuple row : tagRows) {
      UUID contentId = row.get(ct.id.contentId);
      String tagName = row.get(t.name);

      tagsByContentId.computeIfAbsent(contentId, k -> new ArrayList<>()).add(tagName);
    }

    Map<UUID, ContentSummary> summaryByContentId = new HashMap<>();
    for (Map.Entry<UUID, ContentBase> entry : baseByContentId.entrySet()) {
      UUID contentId = entry.getKey();
      ContentBase base = entry.getValue();

      List<String> tags = tagsByContentId.get(contentId);
      if (tags == null) {
        tags = List.of();
      }

      // ContentSummary 생성
      ContentSummary summary =
          ContentSummary.builder()
              .id(base.id)
              .type(base.type)
              .title(base.title)
              .description(base.description)
              .thumbnailUrl(base.thumbnailUrl)
              .tags(tags)
              .averageRating(base.averageRating)
              .reviewCount(base.reviewCount)
              .build();

      summaryByContentId.put(contentId, summary);
    }

    // playlistId별 List 조합
    for (UUID playlistId : missIds) {
      List<UUID> contentIds = contentIdsByPlaylistId.get(playlistId);

      if (contentIds == null || contentIds.isEmpty()) {
        result.put(playlistId, List.of());
        continue;
      }

      List<ContentSummary> summaries = new ArrayList<>();
      for (UUID contentId : contentIds) {
        ContentSummary summary = summaryByContentId.get(contentId);
        if (summary != null) {
          summaries.add(summary);
        }
      }
      result.put(playlistId, summaries);
    }

    // 캐시 저장
    cacheContents(result, missIds);
    return result;
  }

  private void cacheContents(Map<UUID, List<ContentSummary>> result, List<UUID> missIds) {
    for (UUID playlistId : missIds) {
      try {
        redisTemplateForObject
            .opsForValue()
            .set(
                RedisKeyPrefix.PLAYLIST_CONTENTS + playlistId,
                result.getOrDefault(playlistId, List.of()),
                Duration.ofMinutes(30));
      } catch (Exception e) {
        log.debug("Redis 캐시 저장 실패 playlistId={} error={}", playlistId, e.getMessage());
      }
    }
  }

  private record ContentBase(
      UUID id,
      io.mopl.api.content.domain.ContentType type,
      String title,
      String description,
      String thumbnailUrl,
      double averageRating,
      int reviewCount) {}

  public List<ContentSummary> loadContentsByPlaylistId(UUID playlistId) {
    if (playlistId == null) {
      return List.of();
    }
    Map<UUID, List<ContentSummary>> map = loadContentsByPlaylistIds(List.of(playlistId));
    List<ContentSummary> contents = map.get(playlistId);
    return contents != null ? contents : List.of();
  }
}
