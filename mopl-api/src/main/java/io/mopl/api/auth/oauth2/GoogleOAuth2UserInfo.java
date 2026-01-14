package io.mopl.api.auth.oauth2;

// Google OAuth2 API 표준 응답
// {
//    "iss": "https://accounts.google.com",
//    "azp": "1234987819200.apps.googleusercontent.com",
//    "aud": "1234987819200.apps.googleusercontent.com",
//    "sub": "10769150350006150715113082367", ->>> (구글 고유 ID)
//    "at_hash": "HK6E_P6Dh8Y93mRNtsDB1Q",
//    "hd": "example.com",
//    "email": "jsmith@example.com",
//    "email_verified": "true",
//    "iat": 1353601026,
//    "exp": 1353604926,
//    "nonce": "0394852-3190485-2490358"
//    }

// 참고 https://developers.google.com/identity/openid-connect/openid-connect?hl=ko#obtainuserinfo

import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

  private final Map<String, Object> attributes;

  @Override
  public String getProviderId() {
    return (String) attributes.get("sub");
  }

  @Override
  public String getEmail() {
    return (String) attributes.get("email");
  }

  @Override
  public String getName() {
    return (String) attributes.get("name");
  }

  @Override
  public String getProfileImageUrl() {
    return (String) attributes.get("picture");
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }
}
