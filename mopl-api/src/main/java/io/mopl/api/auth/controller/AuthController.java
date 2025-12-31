package io.mopl.api.auth.controller;

import io.mopl.api.auth.dto.AuthTokens;
import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final JwtTokenProvider jwtTokenProvider;

  private static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

  /** 로그인 Content-Type: application/x-www-form-urlencoded */
  @PostMapping(value = "/sign-in", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<JwtDto> signIn(
      @Valid @ModelAttribute SignInRequest request, HttpServletResponse response) {
    AuthTokens authTokens = authService.signIn(request);
    setRefreshTokenCookie(response, authTokens.getRefreshToken());
    return ResponseEntity.ok(authTokens.getJwtDto());
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
    Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(false); // TODO: production 환경에서는 true
    cookie.setPath("/api/auth");
    cookie.setMaxAge((int) jwtTokenProvider.getRefreshTokenValidityInSeconds());
    cookie.setAttribute("SameSite", "Strict");

    response.addCookie(cookie);
  }

  /** CSRF 토큰 조회 */
  @GetMapping("/csrf-token")
  public ResponseEntity<Void> getCsrfToken() {
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
