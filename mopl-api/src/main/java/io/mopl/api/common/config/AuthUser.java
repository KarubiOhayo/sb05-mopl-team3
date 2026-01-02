package io.mopl.api.common.config;

import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class AuthUser extends User {

  private final UUID userId;

  public AuthUser(
      UUID userId,
      String username,
      String password,
      Collection<? extends GrantedAuthority> authorities) {
    super(username, password, authorities);
    this.userId = userId;
  }

  public AuthUser(
      UUID userId, String username, Collection<? extends GrantedAuthority> authorities) {
    super(username, "", authorities);
    this.userId = userId;
  }
}
