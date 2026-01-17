package io.mopl.api.content.event;

import io.mopl.api.content.domain.EventType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContentIndexEvent {
  private UUID contentId;
  private EventType type;
}
