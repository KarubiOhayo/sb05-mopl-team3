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
  EMAIL_SEND_FAILED(500, "error.auth.email-send-failed"),
  TOO_MANY_RESET_REQUESTS(429, "error.auth.too-many-reset-requests"),

  // OAuth2 관련 에러 코드
  OAUTH2_EMAIL_NOT_PROVIDED(400, "error.auth.oauth2.email-not-provided"),
  OAUTH2_EMAIL_ALREADY_REGISTERED(409, "error.auth.oauth2.email-already-registered"),
  OAUTH2_AUTHENTICATION_FAILED(401, "error.auth.oauth2.authentication-failed");

  private final int status;
  private final String messageKey;
}
