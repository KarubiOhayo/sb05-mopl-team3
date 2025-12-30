package io.mopl.api.auth.controller;

import io.mopl.api.auth.dto.AuthTokens;
import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";
  private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 604800; // 7일

  /** 로그인 Content-Type: application/x-www-form-urlencoded */
  @PostMapping(value = "/sign-in", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<JwtDto> signIn(
      @Valid @ModelAttribute SignInRequest request, HttpServletResponse response) {
    log.info("로그인 시도");
    AuthTokens authTokens = authService.signIn(request);

    setRefreshTokenCookie(response, authTokens.getRefreshToken());

    log.info("로그인 성공: userId={}", authTokens.getJwtDto().getUserDto().getId());
    return ResponseEntity.ok(authTokens.getJwtDto());
  }

  /** 토큰 재발급 */
  @PostMapping("/refresh")
  public ResponseEntity<JwtDto> refresh(
      @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME) String refreshToken,
      HttpServletResponse response) {
    log.info("토큰 재발급 요청");
    AuthTokens authTokens = authService.reissueToken(refreshToken);

    setRefreshTokenCookie(response, authTokens.getRefreshToken());

    log.info("토큰 재발급 성공: userId={}", authTokens.getJwtDto().getUserDto().getId());
    return ResponseEntity.ok(authTokens.getJwtDto());
  }

  /** 리프레시 토큰 쿠키 설정 */
  private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
    Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(false);
    cookie.setPath("/api/auth");
    cookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE);

    response.addCookie(cookie);
    log.debug("리프레시 토큰 쿠키 설정 완료");
  }
}
