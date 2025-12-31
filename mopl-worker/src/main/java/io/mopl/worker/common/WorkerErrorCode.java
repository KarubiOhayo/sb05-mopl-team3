package io.mopl.worker.common;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkerErrorCode implements ErrorCode {
  S3_BUCKET_NOT_CONFIGURED(500, "error.worker.s3-bucket-not-configured"),
  THUMBNAIL_DOWNLOAD_FAILED(502, "error.worker.thumbnail-download-failed"),
  THUMBNAIL_EMPTY_BODY(502, "error.worker.thumbnail-empty-body");

  private final int status;
  private final String messageKey;
}
