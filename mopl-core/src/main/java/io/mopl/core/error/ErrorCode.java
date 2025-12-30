package io.mopl.core.error;

public interface ErrorCode {
  int getStatus();

  String getMessageKey();
}
