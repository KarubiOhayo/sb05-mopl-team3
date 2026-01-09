package io.mopl.batch.common;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;

/** 시간 기반 정렬이 가능한 UUID v7 생성 유틸리티. */
public class UuidV7Generator {

  private UuidV7Generator() {}

  /** 현재 시각을 기준으로 정렬 가능한 UUID v7를 생성한다. */
  public static UUID generate() {
    return UuidCreator.getTimeOrderedEpoch();
  }
}
