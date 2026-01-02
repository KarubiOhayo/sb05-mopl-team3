package io.mopl.api.common.config;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class CustomAuthenticationToken extends AbstractAuthenticationToken {

  private final AuthUser principal;

  public CustomAuthenticationToken(
      AuthUser principal, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public AuthUser getPrincipal() {
    return principal;
  }
}
