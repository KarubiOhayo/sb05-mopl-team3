package io.mopl.core.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  INVALID_REQUEST(400, "잘못된 요청입니다."),
  UNAUTHORIZED(401, "인증이 필요합니다."),
  FORBIDDEN(403, "접근 권한이 없습니다."),
  NOT_FOUND(404, "대상을 찾을 수 없습니다."),
  CONFLICT(409, "요청이 현재 상태와 충돌합니다."),
  INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다.");

  private final int status;
  private final String message;
}
