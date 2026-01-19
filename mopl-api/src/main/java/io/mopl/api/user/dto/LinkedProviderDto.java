package io.mopl.api.user.dto;

import io.mopl.api.user.domain.AuthProvider;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkedProviderDto {

  private String provider;
  private String maskedEmail;
  private Instant linkedAt;

  public static LinkedProviderDto of(AuthProvider provider, String maskedEmail, Instant linkedAt) {
    return LinkedProviderDto.builder()
        .provider(provider.name())
        .maskedEmail(maskedEmail)
        .linkedAt(linkedAt)
        .build();
  }
}
