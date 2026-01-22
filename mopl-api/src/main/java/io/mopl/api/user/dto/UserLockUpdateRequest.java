package io.mopl.api.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLockUpdateRequest {

  @NotNull(message = "잠금 상태는 필수입니다")
  private Boolean locked;
}
