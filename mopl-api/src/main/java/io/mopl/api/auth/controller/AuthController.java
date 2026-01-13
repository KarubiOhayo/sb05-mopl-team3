package io.mopl.api.auth.controller;

import io.mopl.api.auth.dto.AuthTokens;
import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.ResetPasswordRequest;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.auth.service.AuthService;
import io.mopl.api.auth.service.RefreshTokenService;
import io.mopl.api.common.config.CookieSecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;
  private final CookieSecurityProperties cookieSecurityProperties;

  // ★★★주의: 이 상수값은 application.yml의 REFRESH_TOKEN_NAME 기본값과 일치해야 함
  private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

  /** 로그인 Content-Type: application/x-www-form-urlencoded */
  @PostMapping(value = "/sign-in", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<JwtDto> signIn(
      @Valid @ModelAttribute SignInRequest request, HttpServletResponse response) {
    AuthTokens authTokens = authService.signIn(request);
    setRefreshTokenCookie(response, authTokens.getRefreshToken());
    return ResponseEntity.ok(authTokens.getJwtDto());
  }

  /** 로그아웃 */
  @PostMapping("/sign-out")
  public ResponseEntity<Void> signOut(
      @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
      HttpServletResponse response) {
    try {
      if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
        UUID userId = jwtTokenProvider.getUserId(refreshToken);
        refreshTokenService.deleteRefreshToken(userId);
      }
    } catch (Exception ignored) {
    } finally {
      clearRefreshTokenCookie(response);
    }

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /** 리프레시 토큰 쿠키 제거 */
  private void clearRefreshTokenCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie(cookieSecurityProperties.getRefreshToken().getName(), null);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecurityProperties.isSecure());
    cookie.setPath("/api/auth");
    cookie.setMaxAge(0);
    cookie.setAttribute("SameSite", cookieSecurityProperties.getSameSite());

    response.addCookie(cookie);
  }

  /** 토큰 재발급 */
  @PostMapping("/refresh")
  public ResponseEntity<JwtDto> refresh(
      @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME) String refreshToken,
      HttpServletResponse response) {
    AuthTokens authTokens = authService.reissueToken(refreshToken);
    setRefreshTokenCookie(response, authTokens.getRefreshToken());
    return ResponseEntity.ok(authTokens.getJwtDto());
  }

  /** 리프레시 토큰 쿠키 설정 */
  private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
    Cookie cookie = new Cookie(cookieSecurityProperties.getRefreshToken().getName(), refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecurityProperties.isSecure());
    cookie.setPath("/api/auth");
    cookie.setMaxAge((int) jwtTokenProvider.getRefreshTokenValidityInSeconds());
    cookie.setAttribute("SameSite", cookieSecurityProperties.getSameSite());

    response.addCookie(cookie);
  }

  /** CSRF 토큰 조회 */
  @GetMapping("/csrf-token")
  public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
    return ResponseEntity.noContent().build();
  }

  /** 비밀번호 초기화 후 이메일 전송 */
  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.noContent().build();
  }
}
