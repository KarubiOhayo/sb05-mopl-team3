package io.mopl.api.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
  ROLE_USER("일반 사용자"),
  ROLE("관리자");

  private final String description;

}
