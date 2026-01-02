package io.mopl.api.common.error;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
  DUPLICATED_EMAIL(409, "error.user.duplicate-email"),
  USER_NOT_FOUND(404, "error.user.not-found"),
  UNAUTHORIZED(403, "error.user.unauthorized"),
  FORBIDDEN(403, "error.user.forbidden"),
  SAME_PASSWORD(400, "error.user.same-password");

  private final int status;
  private final String messageKey;
}
