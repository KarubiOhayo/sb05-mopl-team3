package io.mopl.api.common.error;

import java.util.Map;
import lombok.Getter;

@Getter
public class AuthBusinessException extends RuntimeException {

  private final AuthErrorCode errorCode;
  private final Map<String, String> details;

  public AuthBusinessException(AuthErrorCode errorCode) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
    this.details = Map.of();
  }

  public AuthBusinessException(AuthErrorCode errorCode, Map<String, String> details) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
    this.details = details == null ? Map.of() : Map.copyOf(details);
  }
}
