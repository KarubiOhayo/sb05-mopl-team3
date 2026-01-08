package io.mopl.api.review.error;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {
  ALREADY_EXISTS_REVIEW(409, "error.review.already-exists");

  private final int status;
  private final String messageKey;
}
