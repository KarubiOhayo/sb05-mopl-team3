package io.mopl.api.auth.controller;

import io.mopl.api.auth.dto.AuthTokens;
import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.ResetPasswordRequest;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.auth.service.AuthService;
import io.mopl.api.auth.service.RefreshTokenService;
import io.mopl.api.common.config.CookieSecurityProperties;
import io.mopl.api.common.util.CookieUtils;
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
  private final CookieUtils cookieUtils;

  // ★★★주의: 이 상수값은 application.yml의 REFRESH_TOKEN_NAME 기본값과 일치해야 함
  private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

  /** 로그인 Content-Type: application/x-www-form-urlencoded */
  @PostMapping(value = "/sign-in", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<JwtDto> signIn(
      @Valid @ModelAttribute SignInRequest request, HttpServletResponse response) {
    AuthTokens authTokens = authService.signIn(request);
    cookieUtils.setRefreshTokenCookie(response, authTokens.getRefreshToken());
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
      cookieUtils.clearRefreshTokenCookie(response);
    }

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /** 토큰 재발급 - POST */
  @PostMapping("/refresh")
  public ResponseEntity<JwtDto> refreshPost(
      @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
      HttpServletResponse response) {
    return refreshToken(refreshToken, response);
  }

  /** 공통 토큰 재발급 로직 */
  private ResponseEntity<JwtDto> refreshToken(String refreshToken, HttpServletResponse response) {
    if (refreshToken == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
      AuthTokens authTokens = authService.reissueToken(refreshToken);
      cookieUtils.setRefreshTokenCookie(response, authTokens.getRefreshToken());
      return ResponseEntity.ok(authTokens.getJwtDto());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
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
