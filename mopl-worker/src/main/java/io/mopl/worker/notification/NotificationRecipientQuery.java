package io.mopl.worker.notification;

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

  // 팔로우/구독 알림 수신자 목록을 DB에서 조회한다.
  // TODO: 대량 수신자 대비 배치/페이지네이션 처리 검토
  public List<UUID> findFollowerIds(UUID followeeId) {
    List<String> rows =
        jdbcTemplate.queryForList(
            "SELECT follower_id FROM follows WHERE followee_id = ? LIMIT 10000",
            String.class,
            followeeId.toString());
    return toUuids(rows, "follower_id");
  }

  public List<UUID> findSubscriberIds(UUID playlistId) {
    List<String> rows =
        jdbcTemplate.queryForList(
            "SELECT user_id FROM playlist_subscriptions WHERE playlist_id = ?",
            String.class,
            playlistId.toString());
    return toUuids(rows, "user_id");
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

  private List<UUID> toUuids(List<String> rows, String columnName) {
    List<UUID> results = new ArrayList<>(rows.size());
    int invalidCount = 0;
    for (String value : rows) {
      try {
        results.add(UUID.fromString(value));
      } catch (IllegalArgumentException e) {
        log.warn("UUID 형식이 올바르지 않습니다: {}={}", columnName, value);
        invalidCount++;
      }
    }
    if (invalidCount > 0) {
      log.error("총 {}개의 잘못된 UUID가 필터링되었습니다 (column={})", invalidCount, columnName);
    }
    return results;
  }
}
