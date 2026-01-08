package io.mopl.core.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
  INVALID_REQUEST(400, "error.common.invalid-request"),
  UNAUTHORIZED(401, "error.common.unauthorized"),
  FORBIDDEN(403, "error.common.forbidden"),
  NOT_FOUND(404, "error.common.not-found"),
  CONFLICT(409, "error.common.conflict"),
  INTERNAL_SERVER_ERROR(500, "error.common.internal-server-error");

  private final int status;
  private final String messageKey;
}
