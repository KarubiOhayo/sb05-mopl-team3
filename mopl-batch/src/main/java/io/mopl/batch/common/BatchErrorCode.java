package io.mopl.batch.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BatchErrorCode {
  TMDB_API_CALL_ERROR(400, "TMDB API 호출 중 오류가 발생했습니다.");

  private final int status;
  private final String message;
}
