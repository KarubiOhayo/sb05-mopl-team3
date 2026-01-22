package io.mopl.redis.constants;

public final class RedisKeyPrefix {

  // ===== 인증 (auth:) =====

  /** 리프레시 토큰: auth:refresh-token:{userId} */
  public static final String REFRESH_TOKEN = "auth:refresh-token:";

  /** 사용자 잠금 상태: auth:user-locked:{userId} */
  public static final String USER_LOCKED = "auth:user-locked:";

  /** 임시 비밀번호: auth:temp-password:{userId} */
  public static final String TEMP_PASSWORD = "auth:temp-password:";

  /** 비밀번호 초기화 요청 제한: auth:reset-limit:{hashedEmail} */
  public static final String PASSWORD_RESET_LIMIT = "auth:reset-limit:";

  // ===== 시청 세션 (watching:) =====

  /** 시청 세션: watching:content/user:{contentId/userId} */
  public static final String CONTENT_PREFIX = "watching:content:";

  public static final String USER_PREFIX = "watching:user:";

  public static final String CONTENT_INFO_PREFIX = "watching:content-info:";

  public static final String USER_INFO_PREFIX = "watching:user-info:";

  // ===== playlist =====

  /** 사용자 요약 캐시: user:summary:{userId} */
  public static final String USER_SUMMARY = "user:summary:";

  /** 플레이리스트 콘텐츠 캐시: playlist:contents:{playlistId} */
  public static final String PLAYLIST_CONTENTS = "playlist:contents:";

  public static final String PLAYLIST_THUMBNAIL_CONTENT = "playlist:thumbnail-content:";

  public static final String PLAYLIST_COUNT = "playlist:count:";

  /** 사용자 구독 플레이리스트 Set: playlist:subs:user:{userId} */
  public static final String PLAYLIST_SUBS_BY_USER = "playlist:subs:user:";

  // ===== 알림 (notification:) =====

  /** 미읽음 알림 카운트: notification:unread-count:{userId} */
  public static final String NOTIFICATION_UNREAD_COUNT = "notification:unread-count:";

  /** 미읽음 카운트 중복 방지 키: notification:unread-dedup:{notificationId} */
  public static final String NOTIFICATION_UNREAD_DEDUP = "notification:unread-dedup:";

  /** 인스턴스화 방지 */
  private RedisKeyPrefix() {
    throw new AssertionError("Cannot instantiate constants class");
  }
}
