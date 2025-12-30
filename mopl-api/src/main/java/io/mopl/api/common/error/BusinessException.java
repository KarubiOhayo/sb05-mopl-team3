package io.mopl.api.common.error;

import java.util.Map;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final AuthErrorCode errorCode;
  private final Map<String, String> details;

  public BusinessException(AuthErrorCode errorCode) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
    this.details = Map.of();
  }

  public BusinessException(AuthErrorCode errorCode, Map<String, String> details) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
    this.details = details == null ? Map.of() : Map.copyOf(details);
  }
}
