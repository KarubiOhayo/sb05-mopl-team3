package io.mopl.api.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** OAuth2 로그인 실패 핸들러 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  @Value("${oauth2.redirect-uri:http://localhost:8085}")
  private String redirectUri;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {

    String errorMessage = exception.getLocalizedMessage();

    String targetUrl =
        UriComponentsBuilder.fromUriString(redirectUri)
            .path("/oauth2/redirect")
            .queryParam("error", errorMessage)
            .build()
            .toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}
