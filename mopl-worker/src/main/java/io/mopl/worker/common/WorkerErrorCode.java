package io.mopl.worker.common;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 워커 모듈 전용 에러 코드 정의. */
@Getter
@RequiredArgsConstructor
public enum WorkerErrorCode implements ErrorCode {
  /** S3 버킷 설정이 누락된 경우. */
  S3_BUCKET_NOT_CONFIGURED(500, "error.worker.s3-bucket-not-configured"),
  /** 썸네일 원본 다운로드 실패. */
  THUMBNAIL_DOWNLOAD_FAILED(502, "error.worker.thumbnail-download-failed"),
  /** 썸네일 응답 본문이 비어있는 경우. */
  THUMBNAIL_EMPTY_BODY(502, "error.worker.thumbnail-empty-body");

  private final int status;
  private final String messageKey;
}
