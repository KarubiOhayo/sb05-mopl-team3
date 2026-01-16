package io.mopl.api.auth.oauth2;

import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** OAuth2 인증된 사용자 정보를 담는 클래스 */
@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

  private final User user;
  private final Map<String, Object> attributes;
  private final AuthProvider authProvider;

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
  }

  @Override
  public String getName() {
    return switch (authProvider) {
      case GOOGLE -> (String) attributes.get("sub");
      case KAKAO -> String.valueOf(attributes.get("id"));
      default -> user.getId().toString();
    };
  }

  public String getUserId() {
    return user.getId().toString();
  }

  public String getEmail() {
    return user.getEmail();
  }

  public String getRole() {
    return user.getRole().name();
  }
}
