package io.mopl.api.content.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContentThumbnailDeleteEvent {
	private String deletedUrl;
}
