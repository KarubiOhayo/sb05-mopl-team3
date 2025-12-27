package io.mopl.api.auth.dto;

import io.mopl.api.user.dto.UserDto;
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
