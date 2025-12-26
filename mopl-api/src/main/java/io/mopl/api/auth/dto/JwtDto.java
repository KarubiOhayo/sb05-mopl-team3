package io.mopl.api.auth.dto;

import io.mopl.api.user.domain.UserRole;
import io.mopl.api.user.dto.UserDto;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtDto {

  private UserDto userDto;
  private String accessToken;

}