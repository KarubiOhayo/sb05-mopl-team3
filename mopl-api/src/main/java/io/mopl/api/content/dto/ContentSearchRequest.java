package io.mopl.api.content.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentSearchRequest {
	@Pattern(
		regexp = "^(movie|tvSeries|sport)?$",
		message = "typeEqual must be movie, tvSeries or sport."
	)
	private String typeEqual;
	private String keywordLike;
	private List<String> tagsIn;
	private String cursor;
	private UUID idAfter;

	@Min(10)
	@Max(50)
	private Integer limit;

	@Pattern(
		regexp = "^(ASCENDING|DESCENDING)?$",
		message = "sortDirection must be ASCENDING or DESCENDING."
	)
	private String sortDirection;

	@Pattern(
		regexp = "^(createdAt|watcherCount|rate)?$",
		message = "sortBy must be createdAt or watcherCount or rate."
	)
	private String sortBy;

	public int getLimitOrDefault() {
		return limit != null ? limit : 20;
	}

	public String getSortDirectionOrDefault() {
		return (sortDirection == null || sortDirection.isBlank()) ? "DESCENDING" : sortDirection;
	}

	public String getSortByOrDefault() {
		return (sortBy == null || sortBy.isBlank()) ? "watcherCount" : sortBy;
	}

}
