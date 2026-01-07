package io.mopl.api.follow.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowDto {

  private UUID id;
  private UUID followeeId; // 팔로우 대상 사용자 ID
  private UUID followerId; // 팔로워 사용자 ID
}
