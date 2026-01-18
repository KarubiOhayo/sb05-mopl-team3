package io.mopl.api.auth.oauth2;

import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.auth.service.RefreshTokenService;
import io.mopl.api.common.config.CookieSecurityProperties;
import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.common.util.CookieUtils;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.service.UserLinkedProviderService;
import io.mopl.core.error.BusinessException;
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
  private final CookieUtils cookieUtils;
  private final UserRepository userRepository;

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

    log.debug("=== OAuth2AuthenticationSuccessHandler ===");
    log.debug("state: {}", state);
    log.debug("추출된 mode: {}", mode);
    log.debug("모드 판단: {}", "link".equals(mode) ? "연동 모드" : "로그인 모드");

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
      UUID currentUserId = getCurrentUserIdFromCookie(request);
      log.debug("추출된 userId: {}", currentUserId);

      if (currentUserId == null) {
        log.warn("연동 모드이지만 userId가 null → UNAUTHORIZED");
        sendPopupCloseHtml(response, "error", "UNAUTHORIZED", null);
        return;
      }

      OAuth2AuthenticationToken oAuthToken = (OAuth2AuthenticationToken) authentication;
      String registrationId = oAuthToken.getAuthorizedClientRegistrationId();
      AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

      User socialUser = oAuth2User.getUser();
      String providerUserId = socialUser.getProviderUserId();
      String providerEmail = socialUser.getEmail(); // ✅ 이메일 추가

      // ✅ 4개 파라미터 전달
      linkedProviderService.linkProvider(currentUserId, provider, providerUserId, providerEmail);

      // ✅ 연동 성공 후 현재 사용자 정보로 새로운 액세스 토큰 발급
      User currentUser =
          userRepository
              .findById(currentUserId)
              .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

      String newAccessToken =
          jwtTokenProvider.createAccessToken(
              currentUser.getId(),
              currentUser.getEmail(),
              currentUser.getRole().name(),
              currentUser.getName(),
              currentUser.getProfileImageUrl());

      log.info("연동 성공 - provider: {}, 새 액세스 토큰 발급", provider);
      sendPopupCloseHtml(response, "success", provider.name(), newAccessToken);

    } catch (BusinessException e) {
      log.warn("소셜 계정 연동 실패: {}", e.getMessage());
      String errorCode = ((Enum<?>) e.getErrorCode()).name();
      sendPopupCloseHtml(response, "error", errorCode, null);
    } catch (Exception e) {
      log.error("소셜 계정 연동 중 예외 발생", e);
      sendPopupCloseHtml(response, "error", "INTERNAL_SERVER_ERROR", null);
    }
  }

  /** 로그인 모드 처리 */
  private void handleLoginMode(
      HttpServletRequest request, HttpServletResponse response, CustomOAuth2User oAuth2User)
      throws IOException {
    User user = oAuth2User.getUser();

    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
    refreshTokenService.saveRefreshToken(user.getId(), refreshToken);
    cookieUtils.setRefreshTokenCookie(response, refreshToken);

    CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
    csrfTokenRepository.saveToken(csrfToken, request, response);

    String targetUrl = redirectUri + "/#/contents";

    log.info("로그인 성공 - 리다이렉트: {}", targetUrl);
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

  /** 팝업 창을 닫고 부모 창에 메시지 전송하는 HTML 응답 */
  private void sendPopupCloseHtml(
      HttpServletResponse response, String type, String data, String accessToken)
      throws IOException {
    response.setContentType("text/html;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);

    String html =
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>연동 처리 중...</title>
        </head>
        <body>
            <script>
                if (window.opener && !window.opener.closed) {
                    window.opener.postMessage({
                        type: 'OAUTH_LINK_%s',
                        data: '%s',
                        accessToken: %s
                    }, window.location.origin);
                }
                window.close();
            </script>
            <p>처리 중입니다. 잠시만 기다려주세요...</p>
        </body>
        </html>
        """
            .formatted(
                type.toUpperCase(), data, accessToken != null ? "'" + accessToken + "'" : "null");

    response.getWriter().write(html);
    response.getWriter().flush();
  }
}
