package io.mopl.api.auth.oauth2;

import java.util.Map;

public interface OAuth2UserInfo {

  String getProviderId();

  String getEmail();

  String getName();

  String getProfileImageUrl();

  Map<String, Object> getAttributes();
}
