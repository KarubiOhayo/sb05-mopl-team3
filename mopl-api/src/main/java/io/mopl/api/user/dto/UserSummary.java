package io.mopl.api.user.dto;

import io.mopl.api.user.domain.User;
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

  public static UserSummary from(User user, String presignedProfileImageUrl) {
    return UserSummary.builder()
        .userId(user.getId())
        .name(user.getName())
        .profileImageUrl(presignedProfileImageUrl)
        .build();
  }
}
