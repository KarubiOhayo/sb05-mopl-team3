package io.mopl.batch.client.tsdb.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** TheSportsDB 이벤트 목록 응답 DTO. */
@Getter
@NoArgsConstructor
public class TsdbEventsResponse<T> {
  private List<T> events;
}
