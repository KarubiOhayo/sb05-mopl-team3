package io.mopl.api.content.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ThumbnailUploadedEvent {
  private String thumbnailUrl;
}
