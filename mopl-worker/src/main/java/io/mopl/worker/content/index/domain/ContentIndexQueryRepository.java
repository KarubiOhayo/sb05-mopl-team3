package io.mopl.worker.content.index.domain;

import io.mopl.worker.content.index.dto.ContentIndexRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentIndexQueryRepository {

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public List<ContentIndexRow> findAllForIndexing(List<UUID> contentIds) {
    if (contentIds == null || contentIds.isEmpty()) {
      return List.of();
    }

    List<String> idStrings = contentIds.stream().map(UUID::toString).toList();
    MapSqlParameterSource params = new MapSqlParameterSource("ids", idStrings);

    List<ContentIndexRow> rows =
        jdbcTemplate.query(
            """
            select c.id,
                   c.type,
                   c.title,
                   c.description,
                   c.thumbnail_image_key,
                   c.average_rating,
                   c.review_count,
                   c.watcher_count,
                   c.created_at
              from contents c
             where c.id in (:ids)
            """,
            params,
            (rs, rowNum) -> mapRow(rs));

    if (rows.isEmpty()) {
      return rows;
    }

    Map<UUID, List<String>> tagsByContentId = new HashMap<>();
    jdbcTemplate.query(
        """
        select ct.content_id as content_id, t.name as tag_name
          from content_tags ct
          join tags t on ct.tag_id = t.id
         where ct.content_id in (:ids)
        """,
        params,
        (RowCallbackHandler)
            rs -> {
              UUID contentId = UUID.fromString(rs.getString("content_id"));
              String tagName = rs.getString("tag_name");
              if (tagName == null || tagName.isBlank()) {
                return;
              }
              tagsByContentId.computeIfAbsent(contentId, key -> new ArrayList<>()).add(tagName);
            });

    for (ContentIndexRow row : rows) {
      row.setTags(tagsByContentId.getOrDefault(row.getId(), List.of()));
    }

    return rows;
  }

  private ContentIndexRow mapRow(ResultSet rs) throws SQLException {
    UUID id = UUID.fromString(rs.getString("id"));
    String typeValue = rs.getString("type");
    ContentType type = typeValue != null ? ContentType.valueOf(typeValue) : null;

    Double averageRating = (Double) rs.getObject("average_rating");
    Integer reviewCount = (Integer) rs.getObject("review_count");
    Long watcherCount = (Long) rs.getObject("watcher_count");
    Timestamp createdAt = rs.getTimestamp("created_at");

    return ContentIndexRow.builder()
        .id(id)
        .type(type)
        .title(rs.getString("title"))
        .description(rs.getString("description"))
        .thumbnailImageKey(rs.getString("thumbnail_image_key"))
        .tags(new ArrayList<>())
        .averageRating(averageRating != null ? averageRating : 0.0)
        .reviewCount(reviewCount != null ? reviewCount : 0)
        .watcherCount(watcherCount != null ? watcherCount : 0L)
        .createdAt(createdAt != null ? createdAt.toInstant() : Instant.EPOCH)
        .build();
  }
}
