package io.mopl.api.content.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Document(indexName = "contents")
public class ContentDocument {
	@Id
	private String id;

	private UUID contentId;

	private ContentType type;
	private String title;
	private String description;
	private String thumbnailUrl;
	private List<String> tags;

	private double averageRating;
	private int reviewCount;
	private long watcherCount;

	private Instant createdAt;
}
