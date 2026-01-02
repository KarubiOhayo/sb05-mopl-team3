package io.mopl.api.common.error;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  USER_NOT_FOUND(404, "error.auth.user-not-found"),
  ACCOUNT_LOCKED(403, "error.auth.account-locked"),
  INVALID_PASSWORD(401, "error.auth.invalid-password"),
  INVALID_REFRESH_TOKEN(401, "error.auth.invalid-refresh-token"),
  EMAIL_SEND_FAILED(500, "error.auth.mail-send-failed"),
  ;

  private final int status;
  private final String messageKey;
}
