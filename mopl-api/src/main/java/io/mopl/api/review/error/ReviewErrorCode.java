package io.mopl.api.review.error;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {
  ALREADY_EXISTS_REVIEW(409, "error.review.already-exists"),
  NOT_FOUND_REVIEW(404, "error.review.not-found"),
  NOT_AUTHOR(403, "error.review.not-author");

  private final int status;
  private final String messageKey;
}
