package io.mopl.core.error;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, String> details;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
    this.details = new HashMap<>();
  }

  public BusinessException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessageKey());
    this.errorCode = errorCode;
    this.details = new HashMap<>();
  }

  public BusinessException addDetail(String key, String value) {
    this.details.put(key, value);
    return this;
  }
}
