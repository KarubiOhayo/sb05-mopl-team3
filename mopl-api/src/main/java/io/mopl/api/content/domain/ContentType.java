package io.mopl.api.content.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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
        .orElseThrow(() -> new IllegalArgumentException("Unknown content type: " + value));
  }
}
