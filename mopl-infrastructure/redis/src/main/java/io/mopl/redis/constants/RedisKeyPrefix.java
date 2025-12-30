package io.mopl.redis.constants;

public final class RedisKeyPrefix {

  // ===== 인증 =====

  /** 리프레시 토큰 : rt:{userId} */
  public static final String REFRESH_TOKEN = "rt:";

  /** 이메일 인증 코드: email:verify:{email} */
  public static final String EMAIL_VERIFICATION = "email:verify:";

  /** 인스턴스화 방지 */
  private RedisKeyPrefix() {
    throw new AssertionError("Cannot instantiate constants class");
  }
}
