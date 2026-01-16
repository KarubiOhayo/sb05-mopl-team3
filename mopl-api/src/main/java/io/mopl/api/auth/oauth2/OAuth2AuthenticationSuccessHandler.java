package io.mopl.api.auth.oauth2;

import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.auth.service.RefreshTokenService;
import io.mopl.api.common.config.CookieSecurityProperties;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.service.UserLinkedProviderService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** OAuth2 로그인 성공 핸들러 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;
  private final CookieSecurityProperties cookieSecurityProperties;
  private final CsrfTokenRepository csrfTokenRepository;
  private final UserLinkedProviderService linkedProviderService;

  @Value("${oauth2.redirect-uri:http://localhost:8085}")
  private String redirectUri;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    if (response.isCommitted()) {
      return;
    }

    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
    String state = request.getParameter("state");
    String mode = extractModeFromState(state);

    System.out.println("=== OAuth2AuthenticationSuccessHandler ===");
    System.out.println("state: " + state);
    System.out.println("추출된 mode: " + mode);
    System.out.println("모드 판단: " + ("link".equals(mode) ? "연동 모드" : "로그인 모드"));

    if ("link".equals(mode)) {
      log.info("🔗 연동 모드로 처리");
      handleLinkMode(request, response, oAuth2User, authentication);
    } else {
      log.info("🔐 로그인 모드로 처리");
      handleLoginMode(request, response, oAuth2User);
    }
  }

  /** 연동 모드 처리 */
  private void handleLinkMode(
      HttpServletRequest request,
      HttpServletResponse response,
      CustomOAuth2User oAuth2User,
      Authentication authentication)
      throws IOException {
    try {
      // ✅ 쿠키 디버깅
      Cookie[] cookies = request.getCookies();
      System.out.println("=== 쿠키 확인 ===");
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          System.out.println("쿠키: " + cookie.getName() + " = " + cookie.getValue());
        }
      } else {
        System.out.println("쿠키 없음!");
      }

      UUID currentUserId = getCurrentUserIdFromCookie(request);
      System.out.println("추출된 userId: " + currentUserId);

      if (currentUserId == null) {
        System.out.println("❌ userId가 null → UNAUTHORIZED 리다이렉트");
        redirectToError(response, CommonErrorCode.UNAUTHORIZED.name());
        return;
      }

      OAuth2AuthenticationToken oAuthToken = (OAuth2AuthenticationToken) authentication;
      String registrationId = oAuthToken.getAuthorizedClientRegistrationId();
      AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

      User socialUser = oAuth2User.getUser();
      String providerUserId = socialUser.getProviderUserId();

      linkedProviderService.linkProvider(currentUserId, provider, providerUserId);

      String targetUrl =
          UriComponentsBuilder.fromUriString(redirectUri)
              .fragment("/settings/account")
              .queryParam("linked", provider.name())
              .build()
              .toUriString();

      getRedirectStrategy().sendRedirect(request, response, targetUrl);

    } catch (BusinessException e) {
      log.warn("소셜 계정 연동 실패: {}", e.getMessage());
      String errorCode = ((Enum<?>) e.getErrorCode()).name();
      redirectToError(response, errorCode);
    } catch (Exception e) {
      log.error("소셜 계정 연동 중 예외 발생", e);
      redirectToError(response, CommonErrorCode.INTERNAL_SERVER_ERROR.name());
    }
  }

  /** 로그인 모드 처리 */
  private void handleLoginMode(
      HttpServletRequest request, HttpServletResponse response, CustomOAuth2User oAuth2User)
      throws IOException {
    User user = oAuth2User.getUser();

    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
    refreshTokenService.saveRefreshToken(user.getId(), refreshToken);
    setRefreshTokenCookie(response, refreshToken);

    CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
    csrfTokenRepository.saveToken(csrfToken, request, response);

    String targetUrl =
        UriComponentsBuilder.fromUriString(redirectUri).fragment("/contents").build().toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  /** state 에서 mode 추출 */
  private String extractModeFromState(String state) {
    if (state != null && state.contains(":mode=")) {
      String[] parts = state.split(":mode=");
      if (parts.length > 1) {
        return parts[1];
      }
    }
    return null;
  }

  /** 쿠키에서 현재 사용자 ID 추출 */
  private UUID getCurrentUserIdFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }

    for (Cookie cookie : cookies) {
      if (cookieSecurityProperties.getRefreshToken().getName().equals(cookie.getName())) {
        try {
          return jwtTokenProvider.getUserId(cookie.getValue());
        } catch (Exception e) {
          log.warn("Refresh Token에서 사용자 ID 추출 실패", e);
          return null;
        }
      }
    }
    return null;
  }

  /** 에러 페이지로 리다이렉트 */
  private void redirectToError(HttpServletResponse response, String error) throws IOException {
    String targetUrl =
        UriComponentsBuilder.fromUriString(redirectUri)
            .fragment("/settings/account")
            .queryParam("error", error)
            .build()
            .toUriString();

    response.sendRedirect(targetUrl);
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
