package io.mopl.batch.content.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.mopl.batch.common.BatchErrorCode;
import io.mopl.core.error.BusinessException;
import java.util.Arrays;

/** 콘텐츠 분류를 나타내는 열거형. */
public enum ContentType {
  MOVIE("movie"),
  TV_SERIES("tvSeries"),
  SPORT("sport");

  private final String value;

  ContentType(String value) {
    this.value = value;
  }

  /**
   * 직렬화에 사용할 문자열 값을 반환한다.
   *
   * @return 직렬화용 값
   */
  @JsonValue
  public String getValue() {
    return value;
  }

  /**
   * 직렬화된 문자열 값으로부터 콘텐츠 타입을 복원한다.
   *
   * @param value 직렬화된 문자열 값
   * @return 콘텐츠 타입
   * @throws BusinessException 알 수 없는 값인 경우
   */
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
