package io.mopl.api.common.util;

import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.common.config.CookieSecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtils {

  private final CookieSecurityProperties cookieSecurityProperties;
  private final JwtTokenProvider jwtTokenProvider;

  /** Refresh Token 쿠키 설정 */
  public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
    Cookie cookie = new Cookie(cookieSecurityProperties.getRefreshToken().getName(), refreshToken);

    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecurityProperties.isSecure());
    cookie.setPath("/");
    cookie.setMaxAge((int) jwtTokenProvider.getRefreshTokenValidityInSeconds());
    cookie.setAttribute("SameSite", cookieSecurityProperties.getSameSite());

    response.addCookie(cookie);
  }

  /** Refresh Token 쿠키 제거 */
  public void clearRefreshTokenCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie(cookieSecurityProperties.getRefreshToken().getName(), null);

    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecurityProperties.isSecure());
    cookie.setPath("/");
    cookie.setMaxAge(0);
    cookie.setAttribute("SameSite", cookieSecurityProperties.getSameSite());

    response.addCookie(cookie);
  }
}
