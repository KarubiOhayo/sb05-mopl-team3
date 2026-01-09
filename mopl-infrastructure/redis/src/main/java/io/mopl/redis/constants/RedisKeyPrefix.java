package io.mopl.redis.constants;

public final class RedisKeyPrefix {

  // ===== 인증 =====

  /** 리프레시 토큰 : rt:{userId} */
  public static final String REFRESH_TOKEN = "rt:";

  /** 이메일 인증 코드: email:verify:{email} */
  public static final String EMAIL_VERIFICATION = "email:verify:";

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
