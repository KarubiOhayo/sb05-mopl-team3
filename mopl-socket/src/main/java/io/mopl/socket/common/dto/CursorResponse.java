package io.mopl.socket.common.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CursorResponse<T>(
    List<T> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    SortDirection sortDirection) {}
