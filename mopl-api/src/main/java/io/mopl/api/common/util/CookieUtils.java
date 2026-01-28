package io.mopl.api.common.util;

import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.common.config.CookieSecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

@Slf4j
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

  /** 요청에서 특정 이름의 쿠키를 가져옴 */
  public Optional<Cookie> getCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();

    if (cookies != null && cookies.length > 0) {
      for (Cookie cookie : cookies) {
        if (name.equals(cookie.getName())) {
          return Optional.of(cookie);
        }
      }
    }

    return Optional.empty();
  }

  /** 응답에 쿠키 추가 */
  public void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setPath("/");
    cookie.setHttpOnly(false);
    cookie.setSecure(cookieSecurityProperties.isSecure());
    cookie.setMaxAge(maxAge);

    // SameSite 속성 설정
    String sameSite = cookieSecurityProperties.getSameSite();
    if (sameSite != null && !sameSite.isEmpty()) {
      cookie.setAttribute("SameSite", sameSite);
    }

    response.addCookie(cookie);
  }

  /** 쿠키 삭제 */
  public void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
    Cookie[] cookies = request.getCookies();

    if (cookies != null && cookies.length > 0) {
      for (Cookie cookie : cookies) {
        if (name.equals(cookie.getName())) {
          cookie.setValue("");
          cookie.setPath("/");
          cookie.setMaxAge(0);
          response.addCookie(cookie);
        }
      }
    }
  }

  /** 객체를 Base64 문자열로 직렬화 */
  public String serialize(Object object) {
    return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
  }

  /** Base64 문자열을 객체로 역직렬화 */
  public <T> T deserialize(Cookie cookie, Class<T> cls) {
    try {
      byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie.getValue());
      return cls.cast(SerializationUtils.deserialize(decodedBytes));
    } catch (Exception e) {
      log.error("쿠키 역직렬화 실패: {}", cookie.getName(), e);
      return null;
    }
  }
}
