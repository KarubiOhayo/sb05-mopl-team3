package io.mopl.api.content.dto;

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
public class CursorResponseContentDto {
  private List<ContentDto> data;
  private String nextCursor;
  private UUID nextIdAfter;
  private boolean hasNext;
  private long totalCount;
  private String sortBy;
  private String sortDirection;
}
