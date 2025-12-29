package io.mopl.api.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {
  USER_NOT_FOUND(404, "error.auth.user-not-found"),
  ACCOUNT_LOCKED(403, "error.auth.account-locked"),
  INVALID_PASSWORD(401, "error.auth.invalid-password");

  private final int status;
  private final String messageKey;
}
