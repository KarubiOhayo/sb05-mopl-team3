package io.mopl.api.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

/** CSRF 토큰 쿠키 생성 필터 */
@Slf4j
@Component
public class CsrfCookieFilter extends GenericFilterBean {

  private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    CsrfToken csrfToken = (CsrfToken) httpRequest.getAttribute(CsrfToken.class.getName());

    if (csrfToken != null) {
      String token = csrfToken.getToken();

      Cookie cookie = new Cookie(CSRF_COOKIE_NAME, token);
      cookie.setPath("/");
      cookie.setHttpOnly(false);
      cookie.setSecure(false); // TODO: Production에서는 true
      cookie.setMaxAge(-1);
      cookie.setAttribute("SameSite", "Lax");

      httpResponse.addCookie(cookie);
      log.debug("CSRF 쿠키 설정: {}", token.substring(0, Math.min(10, token.length())) + "...");
    }

    chain.doFilter(request, response);
  }
}
