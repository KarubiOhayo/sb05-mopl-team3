package io.mopl.api.auth.oauth2;

public interface OAuth2UserInfo {

  String getProviderId();

  String getEmail();

  String getName();

  String getProfileImageUrl();
}
