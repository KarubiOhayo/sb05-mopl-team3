package io.mopl.worker.thumbnail;

final class RunIdResolver {

  private static final String PREFIX = "thumbnails/bench/";

  private RunIdResolver() {}

  static String resolveFromKey(String s3Key) {
    if (s3Key == null || s3Key.isBlank()) {
      return "none";
    }
    if (!s3Key.startsWith(PREFIX)) {
      return "none";
    }
    int start = PREFIX.length();
    int nextSlash = s3Key.indexOf('/', start);
    if (nextSlash <= start) {
      return "none";
    }
    return s3Key.substring(start, nextSlash);
  }
}
