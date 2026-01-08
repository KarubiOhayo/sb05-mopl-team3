package io.mopl.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthTokens {

  private JwtDto jwtDto;
  private String refreshToken;
}
