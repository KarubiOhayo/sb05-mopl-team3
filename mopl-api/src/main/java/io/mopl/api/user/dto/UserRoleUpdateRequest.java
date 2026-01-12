package io.mopl.api.user.dto;

import io.mopl.api.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateRequest {

  @NotNull(message = "권한은 필수입니다")
  private UserRole role;
}
