package io.mopl.api.common.error;

import io.mopl.core.error.ErrorCode;
import java.util.Map;
import lombok.Getter;

@Getter
public class ApiBusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, String> details;

  public ApiBusinessException(ErrorCode errorCode) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
    this.details = Map.of();
  }

  public ApiBusinessException(ErrorCode errorCode, Map<String, String> details) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
    this.details = details == null ? Map.of() : Map.copyOf(details);
  }
}
