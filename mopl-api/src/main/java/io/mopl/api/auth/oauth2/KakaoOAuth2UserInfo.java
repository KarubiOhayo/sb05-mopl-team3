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
    String name = getName();

    String sanitizedName = name.replaceAll("[^a-zA-Z0-9가-힣]", "");

    return sanitizedName + "_" + providedId + "@kakao.com";
  }
}

// 저희가 회원가입 할때 이메일 인증을 안받음
// 이메일 인증을 받는다는 가정 하에...
// 내 이메일이 아니지만, 내 이메일 주소인 것처럼 사용가능

// wldls3866@gmail.com (이게 본인이 아님, 다른 사람이 가입한 이메일이라면)
// -> 이미 가입해둠.
// -> 소셜 로그인으로 로그인하려고 하면 어떻게 되지? (로그인 ok) 비밀번호 몰라도,
// -> 연동하시겠습니까? (일반적인 패턴...)
// -> 구글 가입을 할때도 {이메일}_{사용자 ID}@gmail.com 이렇게 만들면?

// 그 반대 상황?
// 소셜 로그인을 해 둔 상태에서
// 저 이메일로 다시 가입을 하려고 하는 상황?

// 할 수 있으면 하면 좋지 않을까?
// --> 이미 가입된 계정에 소셜 로그인을 연동할 수 있으면 좋지 않을까?
// --> 프론트 코드가 필요 (필요하면 AI한테 좀 만져달라고 해서... 부탁해볼까?)
// 하게되면? DB 구조 변경될것
