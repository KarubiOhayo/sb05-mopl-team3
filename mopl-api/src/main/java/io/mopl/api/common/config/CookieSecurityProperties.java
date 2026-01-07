package io.mopl.api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** application.yml의 security.cookie 설정을 읽어오는 클래스 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.cookie")
public class CookieSecurityProperties {

  /** HTTPS에서만 쿠키 전송 여부 (프로덕션: true, 개발: false) */
  private boolean secure = false;

  /** SameSite 속성 (Strict, Lax, None) */
  private String sameSite = "Lax";

  /** CSRF 토큰 쿠키 설정 */
  private CsrfCookie csrf = new CsrfCookie();

  /** Refresh 토큰 쿠키 설정 */
  private RefreshTokenCookie refreshToken = new RefreshTokenCookie();

  @Getter
  @Setter
  public static class CsrfCookie {
    private String name = "XSRF-TOKEN";
    private boolean httpOnly = false;
  }

  @Getter
  @Setter
  public static class RefreshTokenCookie {
    private String name = "REFRESH_TOKEN";
    private boolean httpOnly = true;
    private int maxAge = 604800; // 7일
  }
}
