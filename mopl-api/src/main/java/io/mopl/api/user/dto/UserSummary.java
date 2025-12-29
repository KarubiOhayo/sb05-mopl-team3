package io.mopl.api.user.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummary {

  private UUID userId;
  private String name;
  private String profileImageUrl;
}
