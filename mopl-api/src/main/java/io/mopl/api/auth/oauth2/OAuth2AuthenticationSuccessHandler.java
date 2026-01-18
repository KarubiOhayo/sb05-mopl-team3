package io.mopl.api.auth.oauth2;

import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.auth.service.RefreshTokenService;
import io.mopl.api.common.config.CookieSecurityProperties;
import io.mopl.api.common.util.CookieUtils;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
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

    if ("link".equals(mode)) {
      handleLinkMode(request, response, oAuth2User, authentication);
    } else {
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

      if (currentUserId == null) {
        sendPopupCloseHtml(response, "error", "UNAUTHORIZED", null);
        return;
      }

      OAuth2AuthenticationToken oAuthToken = (OAuth2AuthenticationToken) authentication;
      String registrationId = oAuthToken.getAuthorizedClientRegistrationId();
      AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

      User socialUser = oAuth2User.getUser();
      String providerUserId = socialUser.getProviderUserId();
      String providerEmail = socialUser.getEmail();

      linkedProviderService.linkProvider(currentUserId, provider, providerUserId, providerEmail);

      // ✅ CSRF 토큰 재발급만 하면 됨!
      CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
      csrfTokenRepository.saveToken(csrfToken, request, response);

      sendPopupCloseHtml(response, "success", provider.name(), null);

    } catch (BusinessException e) {
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
          return null;
        }
      }
    }
    return null;
  }

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
                console.log('팝업 HTML 실행됨:', '%s', '%s');

                try {
                    if (window.opener && !window.opener.closed) {
                        console.log('부모 창 존재 확인');

                        window.opener.postMessage({
                            type: 'OAUTH_LINK_%s',
                            data: '%s',
                            accessToken: %s
                        }, '%s');

                        console.log('postMessage 전송 완료');
                        // ✅ reload() 호출 제거! 프론트엔드가 처리하도록
                    } else {
                        console.error('부모 창을 찾을 수 없음');
                    }
                } catch (error) {
                    console.error('팝업 처리 중 오류:', error);
                }

                setTimeout(function() {
                    console.log('팝업 닫기');
                    window.close();
                }, 1000);
            </script>
            <p>처리 중입니다. 잠시만 기다려주세요...</p>
        </body>
        </html>
        """
            .formatted(
                type,
                data,
                type.toUpperCase(),
                data,
                accessToken != null ? "'" + accessToken + "'" : "null",
                redirectUri);

    response.getWriter().write(html);
    response.getWriter().flush();
  }
}
