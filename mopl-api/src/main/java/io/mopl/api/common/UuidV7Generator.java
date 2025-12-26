package io.mopl.api.common;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/** UUID v7 유틸리티 클래스 시간 기반 정렬 가능한 UUID 생성 */
public class UuidV7Generator {

  private UuidV7Generator() {}

  public static UUID generate() {
    return UuidCreator.getTimeOrderedEpoch();
  }
}
