package io.mopl.batch.common;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BatchErrorCode implements ErrorCode {
  TMDB_API_CALL_ERROR(502, "error.batch.tmdb-api-call-error"),
  JOB_LAUNCH_FAILED(500, "error.batch.job-launch-failed"),
  UNKNOWN_CONTENT_TYPE(400, "error.batch.unknown-content-type");

  private final int status;
  private final String messageKey;
}
