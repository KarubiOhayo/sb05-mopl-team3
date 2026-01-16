package io.mopl.api.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@RequiredArgsConstructor
public class CsrfConfig {

  private final CookieSecurityProperties cookieSecurityProperties;

  @Bean
  public CookieCsrfTokenRepository csrfTokenRepository() {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();

    repository.setCookieName(cookieSecurityProperties.getCsrf().getName());
    repository.setHeaderName("X-XSRF-TOKEN");

    return repository;
  }
}
