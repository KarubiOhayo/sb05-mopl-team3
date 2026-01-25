package io.mopl.api.review.service.cache;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.common.dto.SortDirection;
import io.mopl.api.review.dto.ReviewDto;
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
public class ReviewPageCache {

  private List<ReviewDto> data;
  private String nextCursor;
  private UUID nextIdAfter;
  private boolean hasNext;
  private long totalCount;
  private String sortBy;
  private SortDirection sortDirection;

  public static ReviewPageCache from(CursorResponse<ReviewDto> response) {
    if (response == null) {
      return null;
    }
    return ReviewPageCache.builder()
        .data(response.getData())
        .nextCursor(response.getNextCursor())
        .nextIdAfter(response.getNextIdAfter())
        .hasNext(response.isHasNext())
        .totalCount(response.getTotalCount())
        .sortBy(response.getSortBy())
        .sortDirection(response.getSortDirection())
        .build();
  }

  public CursorResponse<ReviewDto> toCursorResponse() {
    return CursorResponse.<ReviewDto>builder()
        .data(data)
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .hasNext(hasNext)
        .totalCount(totalCount)
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .build();
  }
}
