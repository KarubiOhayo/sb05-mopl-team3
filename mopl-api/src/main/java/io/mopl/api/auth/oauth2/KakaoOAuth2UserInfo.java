package io.mopl.api.auth.oauth2;

// Kakao OAuth2 API 표준 응답
// {
//    "id":123456789,
//    "connected_at": "2022-04-11T01:45:28Z",
//    "kakao_account": {
//    "profile_nickname_needs_agreement": false,
//    "profile": {
//    "nickname": "홍길동"
//    }
//    },
//    "properties":{
//    "${CUSTOM_PROPERTY_KEY}": "${CUSTOM_PROPERTY_VALUE}",
//    ...
//    }
//    }
// 참고 https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info-response-body

import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

  private final Map<String, Object> attributes;

  @Override
  public String getProviderId() {
    Object id = attributes.get("id");
    return id != null ? String.valueOf(id) : null;
  }

  @Override
  public String getEmail() {
    Map<String, Object> kakaoAccount = getKakaoAccount();
    if (kakaoAccount == null) {
      return generateVirtualEmail();
    }

    String email = (String) kakaoAccount.get("email");
    if (email == null || email.isBlank()) {
      return generateVirtualEmail();
    }

    return email;
  }

  @Override
  public String getName() {
    Map<String, Object> profile = getProfile();
    if (profile == null) {
      return "카카오 사용자";
    }

    String nickname = (String) profile.get("nickname");
    return nickname != null ? nickname : "카카오 사용자";
  }

  @Override
  public String getProfileImageUrl() {
    Map<String, Object> profile = getProfile();
    if (profile == null) {
      return null;
    }
    return (String) profile.get("profile_image_url");
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  /** kakao_account 정보 추출 */
  @SuppressWarnings("unchecked")
  private Map<String, Object> getKakaoAccount() {
    return (Map<String, Object>) attributes.get("kakao_account");
  }

  /** profile 정보 추출 */
  @SuppressWarnings("unchecked")
  private Map<String, Object> getProfile() {
    Map<String, Object> kakaoAccount = getKakaoAccount();
    if (kakaoAccount == null) {
      return null;
    }
    return (Map<String, Object>) kakaoAccount.get("profile");
  }

  /** 가상 이메일 생성, 형식: {닉네임}_{회원 ID}@kakao.com */
  private String generateVirtualEmail() {
    String providedId = getProviderId();
    if (providedId == null) {
      providedId = java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    String name = getName();
    String sanitizedName = name.replaceAll("[^a-zA-Z0-9가-힣]", "");
    return sanitizedName + "_" + providedId + "@kakao.com";
  }
}
