package io.mopl.batch.content.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.mopl.batch.common.BatchErrorCode;
import io.mopl.core.error.BusinessException;
import java.util.Arrays;

public enum ContentType {
  MOVIE("movie"),
  TV_SERIES("tvSeries"),
  SPORT("sport");

  private final String value;

  ContentType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static ContentType fromValue(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(type -> type.value.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessException(BatchErrorCode.UNKNOWN_CONTENT_TYPE)
                    .addDetail("value", value));
  }
}
