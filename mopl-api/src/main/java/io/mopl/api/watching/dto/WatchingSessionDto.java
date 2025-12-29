package io.mopl.api.watching.dto;

import io.mopl.api.content.dto.ContentSummary;
import io.mopl.api.user.dto.UserSummary;
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
public class WatchingSessionDto {

  private UUID id;
  private Instant createdAt;
  private UserSummary watcher;
  private ContentSummary content;
}
