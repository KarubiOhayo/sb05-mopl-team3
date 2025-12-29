package io.mopl.api.common.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursorResponse<T> {

  private List<T> data;
  private String nextCursor;
  private UUID nextIdAfter;
  private boolean hasNext;
  private long totalCount;
  private String sortBy;
  private SortDirection sortDirection;
}
