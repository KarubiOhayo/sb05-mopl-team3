package io.mopl.batch.thumbnail;

import java.util.Locale;

/** 썸네일 업로드 방식. */
public enum ThumbnailUploadMode {
  ASYNC,
  SYNC;

  public static ThumbnailUploadMode fromJobParameter(String value) {
    if (value == null || value.isBlank()) {
      return ASYNC;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if ("sync".equals(normalized)) {
      return SYNC;
    }
    return ASYNC;
  }
}
