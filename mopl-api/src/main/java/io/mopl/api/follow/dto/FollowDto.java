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
  private UUID followeeId;
  private UUID followerId;
}
