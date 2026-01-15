package io.mopl.api.content.event;

import java.util.UUID;

import io.mopl.api.content.domain.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContentIndexEvent {
	private UUID contentId;
	private EventType type;

}
