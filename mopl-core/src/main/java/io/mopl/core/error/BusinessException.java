package io.mopl.core.error;

import java.util.Map;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, String> details;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.details = Map.of();
  }

  public BusinessException(ErrorCode errorCode, Map<String, String> details) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.details = details == null ? Map.of() : Map.copyOf(details);
  }
}
