package io.mopl.api.content.dto;

import io.mopl.api.content.domain.Content;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContentPage {
  private final List<Content> contents;
  private final boolean hasNext;
  private final String nextCursor;
  private final UUID nextIdAfter;
}
