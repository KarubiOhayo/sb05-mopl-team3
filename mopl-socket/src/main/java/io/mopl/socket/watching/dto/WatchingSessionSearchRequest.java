package io.mopl.socket.watching.dto;

import io.mopl.socket.common.dto.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionSearchRequest(
    String watcherNameLike,
    String cursor,
    UUID idAfter,
    @Min(value = 1, message = "{validation.limit.min}")
        @Max(value = 100, message = "{validation.limit.max}")
        Integer limit,
    SortDirection sortDirection,
    @Pattern(regexp = "createdAt", message = "{validation.sortBy.pattern}") String sortBy) {

  public WatchingSessionSearchRequest {
    if (limit == null) {
      limit = 20;
    }
    if (sortDirection == null) {
      sortDirection = SortDirection.DESCENDING;
    }
    if (sortBy == null) {
      sortBy = "createdAt";
    }
  }
}
