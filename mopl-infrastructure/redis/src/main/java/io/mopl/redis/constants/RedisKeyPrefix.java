package io.mopl.redis.constants;

public final class RedisKeyPrefix {

  // ===== 인증 =====

  /** 리프레시 토큰: rt:{userId} */
  public static final String REFRESH_TOKEN = "rt:";

  /** 사용자 잠금 상태: user:locked:{userId} */
  public static final String USER_LOCKED = "user:locked:";

  /** 인스턴스화 방지 */
  private RedisKeyPrefix() {
    throw new AssertionError("Cannot instantiate constants class");
  }
}
