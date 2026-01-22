package io.mopl.worker.notification;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRecipientQuery {

  private final JdbcTemplate jdbcTemplate;

  // 팔로워 알림 수신자를 커서 기반으로 조회한다.
  public RecipientPage findFollowerIdsPage(
      UUID followeeId, Instant cursorCreatedAt, String cursorId, int limit) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT follower_id AS receiver_id, created_at, id AS cursor_id "
                + "FROM follows WHERE followee_id = ?");
    List<Object> params = new ArrayList<>();
    params.add(followeeId.toString());

    if (cursorCreatedAt != null && cursorId != null) {
      sql.append(" AND (created_at > ? OR (created_at = ? AND id > ?))");
      params.add(Timestamp.from(cursorCreatedAt));
      params.add(Timestamp.from(cursorCreatedAt));
      params.add(cursorId);
    }

    sql.append(" ORDER BY created_at ASC, id ASC LIMIT ?");
    params.add(limit + 1);

    List<RecipientRow> rows =
        jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) ->
                new RecipientRow(
                    rs.getString("receiver_id"),
                    toInstant(rs.getTimestamp("created_at")),
                    rs.getString("cursor_id")),
            params.toArray());

    return toRecipientPage(rows, limit, "follower_id");
  }

  // 플레이리스트 구독자 알림 수신자를 커서 기반으로 조회한다.
  public RecipientPage findSubscriberIdsPage(
      UUID playlistId, Instant cursorCreatedAt, String cursorUserId, int limit) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT user_id AS receiver_id, created_at, user_id AS cursor_id "
                + "FROM playlist_subscriptions WHERE playlist_id = ?");
    List<Object> params = new ArrayList<>();
    params.add(playlistId.toString());

    if (cursorCreatedAt != null && cursorUserId != null) {
      sql.append(" AND (created_at > ? OR (created_at = ? AND user_id > ?))");
      params.add(Timestamp.from(cursorCreatedAt));
      params.add(Timestamp.from(cursorCreatedAt));
      params.add(cursorUserId);
    }

    sql.append(" ORDER BY created_at ASC, user_id ASC LIMIT ?");
    params.add(limit + 1);

    List<RecipientRow> rows =
        jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) ->
                new RecipientRow(
                    rs.getString("receiver_id"),
                    toInstant(rs.getTimestamp("created_at")),
                    rs.getString("cursor_id")),
            params.toArray());

    return toRecipientPage(rows, limit, "user_id");
  }

  // 시청 알림 메시지에 사용할 콘텐츠 제목을 조회한다.
  public String findContentTitle(UUID contentId) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT title FROM contents WHERE id = ?", String.class, contentId.toString());
    } catch (EmptyResultDataAccessException e) {
      log.warn("콘텐츠를 찾을 수 없음: contentId={}", contentId);
      return null;
    }
  }

  private RecipientPage toRecipientPage(List<RecipientRow> rows, int limit, String columnName) {
    if (rows.isEmpty()) {
      return new RecipientPage(List.of(), null, null, false);
    }

    boolean hasNext = rows.size() > limit;
    List<RecipientRow> slice = hasNext ? rows.subList(0, limit) : rows;

    List<UUID> results = new ArrayList<>(slice.size());
    int invalidCount = 0;
    for (RecipientRow row : slice) {
      try {
        results.add(UUID.fromString(row.receiverId()));
      } catch (IllegalArgumentException e) {
        log.warn("UUID 형식이 올바르지 않습니다: {}={}", columnName, row.receiverId());
        invalidCount++;
      }
    }
    if (invalidCount > 0) {
      log.error("총 {}개의 잘못된 UUID가 포함되어 있습니다 (column={})", invalidCount, columnName);
    }

    RecipientRow last = slice.get(slice.size() - 1);
    return new RecipientPage(results, last.createdAt(), last.cursorId(), hasNext);
  }

  private Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private record RecipientRow(String receiverId, Instant createdAt, String cursorId) {}

  public record RecipientPage(
      List<UUID> receiverIds, Instant nextCreatedAt, String nextCursorId, boolean hasNext) {}
}
