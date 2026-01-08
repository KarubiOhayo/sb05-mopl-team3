package io.mopl.socket.common.error;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SocketErrorCode implements ErrorCode {
  INVALID_TOKEN(401, "socket.error.token.invalid"),
  MISSING_AUTHENTICATION(401, "socket.error.authentication.missing");

  private final int status;
  private final String messageKey;
}
