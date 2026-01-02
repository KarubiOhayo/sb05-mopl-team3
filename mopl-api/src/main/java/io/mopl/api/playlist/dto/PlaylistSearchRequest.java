package io.mopl.api.playlist.dto;

import java.util.UUID;
import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaylistSearchRequest {

	private String keywordLike;
	private UUID ownerIdEqual;
	private UUID subscriberIdEqual;
	private String cursor;
	private UUID idAfter;

	@Min(1)
	@Max(100)
	private Integer limit;

	@Pattern(regexp = "^(ASCENDING|DESCENDING)$", message = "sortDirection must be ASCENDING or DESCENDING")
	private String sortDirection; // ASCENDING | DESCENDING
	@Pattern(regexp = "^(updatedAt|subscribeCount)$", message = "sortBy must be updatedAt or subscribeCount")
	private String sortBy;        // updatedAt | subscribeCount

	public int getLimitOrDefault() {
		return limit != null ? limit : 20;
	}

	public String getSortDirectionOrDefault() {
		return (sortDirection == null || sortDirection.isBlank()) ? "DESCENDING" : sortDirection;
	}

	public String getSortByOrDefault() {
		return (sortBy == null || sortBy.isBlank()) ? "updatedAt" : sortBy;
	}

	@AssertTrue(message = "cursor and idAfter must be provided together")
	public boolean isCursorAndIdAfterValid() {
		boolean hasCursor = cursor != null && !cursor.isBlank();
		boolean hasIdAfter = idAfter != null;
		return (hasCursor && hasIdAfter) || (!hasCursor && !hasIdAfter);
	}

	@AssertTrue(message = "cursor format must match sortBy")
	public boolean isCursorFormatValid() {
		if (cursor == null || cursor.isBlank()) {
			return true;
		}
		String sort = getSortByOrDefault();
		try {
			if ("updatedAt".equals(sort)) {
				Instant.parse(cursor);
				return true;
			}
			if ("subscribeCount".equals(sort)) {
				Long.parseLong(cursor);
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

}
