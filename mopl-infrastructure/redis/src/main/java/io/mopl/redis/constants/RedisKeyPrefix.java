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

  /** 인스턴스화 방지 */
  private RedisKeyPrefix() {
    throw new AssertionError("Cannot instantiate constants class");
  }
}
