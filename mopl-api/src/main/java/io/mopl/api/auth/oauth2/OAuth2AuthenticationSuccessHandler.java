package io.mopl.api.auth.oauth2;

import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.auth.service.RefreshTokenService;
import io.mopl.api.common.config.CookieSecurityProperties;
import io.mopl.api.user.domain.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** OAuth2 로그인 성공 핸들러 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;
  private final CookieSecurityProperties cookieSecurityProperties;

  @Value("${oauth2.redirect-uri:http://localhost:8085")
  private String redirectUri;

  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    if (response.isCommitted()) {
      return;
    }

    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
    User user = oAuth2User.getUser();

    String accessToken =
        jwtTokenProvider.createAccessToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name(),
            user.getName(),
            user.getProfileImageUrl());

    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
    refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

    setRefreshTokenCookie(response, refreshToken);

    String targetUrl = buildRedirectUrl(accessToken);

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  /** Refresh Token 쿠키 설정 */
  private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
    Cookie cookie = new Cookie(cookieSecurityProperties.getRefreshToken().getName(), refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(cookieSecurityProperties.isSecure());
    cookie.setPath("/api/auth");
    cookie.setMaxAge((int) jwtTokenProvider.getRefreshTokenValidityInSeconds());
    cookie.setAttribute("SameSite", cookieSecurityProperties.getSameSite());

    response.addCookie(cookie);
  }

  /** 리다이렉트 URL 생성 */
  private String buildRedirectUrl(String accessToken) {
    return UriComponentsBuilder.fromUriString(redirectUri)
        .path("/oauth2/redirect")
        .queryParam("token", accessToken)
        .build()
        .toUriString();
  }
}
