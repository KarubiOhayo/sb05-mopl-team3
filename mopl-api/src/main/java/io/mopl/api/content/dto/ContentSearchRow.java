package io.mopl.api.content.dto;

import java.util.List;
import java.util.UUID;

import io.mopl.api.content.domain.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentSearchRow {
	private UUID id;
	private ContentType type;
	private String title;
	private String description;
	private String thumbnailImageKey;
	private List<String> tags;
	private double averageRating;
	private int reviewCount;
	private long watcherCount;
	java.time.Instant createdAt;
}
