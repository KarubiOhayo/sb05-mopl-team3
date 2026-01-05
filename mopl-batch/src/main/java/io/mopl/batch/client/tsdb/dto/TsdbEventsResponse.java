package io.mopl.batch.client.tsdb.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TsdbEventsResponse<T> {
  private List<T> events;
}
