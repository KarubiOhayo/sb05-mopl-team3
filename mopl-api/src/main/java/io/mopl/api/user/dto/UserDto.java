package io.mopl.api.user.dto;

import io.mopl.api.user.domain.UserRole;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

  private UUID id;
  private Instant createdAt;
  private String email;
  private String name;
  private String profileImageUrl;
  private UserRole role;
  private boolean locked;
}
