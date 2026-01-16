package io.mopl.api.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {
  LOCAL("일반"),
  GOOGLE("구글"),
  KAKAO("카카오");

  private final String displayName;
}
