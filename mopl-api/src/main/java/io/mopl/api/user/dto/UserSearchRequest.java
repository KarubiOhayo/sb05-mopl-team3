package io.mopl.api.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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

  public int getLimitOrDefault() {
    return limit != null ? limit : 20;
  }

  public String getSortDirectionOrDefault() {
    return (sortDirection == null || sortDirection.isBlank()) ? "DESCENDING" : sortDirection;
  }

  public String getSortByOrDefault() {
    return (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
  }
}
