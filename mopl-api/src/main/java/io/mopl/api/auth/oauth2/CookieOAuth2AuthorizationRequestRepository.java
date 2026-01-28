package io.mopl.api.auth.oauth2;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import io.mopl.api.auth.dto.OAuth2AuthorizationRequestDto;
import io.mopl.api.common.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CookieOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
  public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
  private static final int COOKIE_EXPIRE_SECONDS = 180; // 3분

  private final CookieUtils cookieUtils;

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return cookieUtils
        .getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
        .map(
            cookie -> {
              OAuth2AuthorizationRequestDto dto =
                  cookieUtils.deserialize(cookie, OAuth2AuthorizationRequestDto.class);

              if (dto != null) {
                return dto.toOAuth2AuthorizationRequest();
              }

              log.warn("OAuth2 Authorization Request DTO 역직렬화 실패");
              return null;
            })
        .orElse(null);
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {

    if (authorizationRequest == null) {
      cookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
      cookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
      return;
    }

    log.debug("OAuth2 Authorization Request 저장: state={}", authorizationRequest.getState());

    try {
      OAuth2AuthorizationRequestDto dto = OAuth2AuthorizationRequestDto.from(authorizationRequest);

      cookieUtils.addCookie(
          response,
          OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
          cookieUtils.serialize(dto),
          COOKIE_EXPIRE_SECONDS);
    } catch (Exception e) {
      log.error("OAuth2 Authorization Request 저장 실패", e);
      cookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
      throw new IllegalStateException("OAuth2 인증 요청 저장에 실패했습니다", e);
    }

    String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
    if (StringUtils.isNotBlank(redirectUriAfterLogin)) {
      cookieUtils.addCookie(
          response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
    }
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    log.debug("OAuth2 Authorization Request 제거");
    OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);

    if (authorizationRequest != null) {
      cookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
      cookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }

    return authorizationRequest;
  }

  /** Authorization Request 쿠키 삭제 */
  public void removeAuthorizationRequestCookies(
      HttpServletRequest request, HttpServletResponse response) {
    cookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
    cookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
  }
}
