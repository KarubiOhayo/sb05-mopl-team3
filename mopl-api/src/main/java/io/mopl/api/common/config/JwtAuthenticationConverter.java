package io.mopl.api.common.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    String username = jwt.getClaim("username");

    List<String> roles = jwt.getClaimAsStringList("roles");
    Collection<GrantedAuthority> authorities =
        roles != null
            ? roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
            : Collections.emptyList();

    AuthUser authUser = new AuthUser(userId, username, authorities);

    return new CustomAuthenticationToken(authUser, authorities);
  }
}
