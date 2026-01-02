package io.mopl.api.common.error;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentErrorCode implements ErrorCode {
  CONTENT_NOT_FOUND(404, "error.content.not-found");

  private final int status;
  private final String messageKey;
}
