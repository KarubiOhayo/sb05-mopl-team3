package io.mopl.api.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSearchRequest {

  private String emailLike;
  private String roleEqual; // USER | ADMIN
  private Boolean isLocked;
  private String cursor;
  private UUID idAfter;

  @Min(1)
  @Max(100)
  private Integer limit;

  @Pattern(
      regexp = "^(ASCENDING|DESCENDING)$",
      message = "정렬 방향은 ASCENDING 이거나 DESCENDING 이어야 합니다.")
  private String sortDirection;

  @Pattern(
      regexp = "^(name|email|createdAt|isLocked|role)$",
      message = "정렬은 name, email, createdAt, isLocked, role 중 하나로만 가능합니다.")
  private String sortBy;

  // ===== 기본값 제공 메서드 =====
  public int getLimitOrDefault() {
    return limit != null ? limit : 20;
  }

  public String getSortDirectionOrDefault() {
    return (sortDirection == null || sortDirection.isBlank()) ? "DESCENDING" : sortDirection;
  }

  public String getSortByOrDefault() {
    return (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
  }

  // ===== 커스텀 Validation =====
  @AssertTrue(message = "cursor와 idAfter은 둘 다 있거나 둘 다 없어야 합니다.")
  public boolean isCursorAndIdAfterValid() {
    boolean hasCursor = cursor != null && !cursor.isBlank();
    boolean hasIdAfter = idAfter != null;
    return (hasCursor && hasIdAfter) || (!hasCursor && !hasIdAfter);
  }

  @AssertTrue(message = "cursor 형식은 sortBy 형식과 매치되어야 합니다.")
  public boolean isCursorFormatValid() {
    if (cursor == null || cursor.isBlank()) {
      return true;
    }
    String sort = getSortByOrDefault();
    try {
      if ("createdAt".equals(sort)) {
        Instant.parse(cursor);
        return true;
      } else if ("isLocked".equals(sort)) {
        if (!"true".equals(cursor) && !"false".equals(cursor)) {
          return false;
        }
        return true;
      } else {
        return !cursor.isBlank();
      }
    } catch (Exception e) {
      return false;
    }
  }
}
