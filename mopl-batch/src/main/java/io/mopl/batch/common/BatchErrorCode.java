package io.mopl.batch.common;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 배치 모듈 전용 에러 코드 정의. */
@Getter
@RequiredArgsConstructor
public enum BatchErrorCode implements ErrorCode {
  /** TMDB API 호출 실패. */
  TMDB_API_CALL_ERROR(502, "error.batch.tmdb-api-call-error"),
  /** 배치 잡 실행 실패. */
  JOB_LAUNCH_FAILED(500, "error.batch.job-launch-failed"),
  /** 알 수 없는 콘텐츠 타입 요청. */
  UNKNOWN_CONTENT_TYPE(400, "error.batch.unknown-content-type"),
  /** TheSportsDB API 호출 실패. */
  TSDB_API_CALL_ERROR(502, "error.batch.tsdb-api-call-error");

  private final int status;
  private final String messageKey;
}
