package io.mopl.api.playlist.dto;

import io.mopl.api.content.dto.ContentSummary;
import io.mopl.api.user.dto.UserSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistDto {

  private UUID id;
  private UserSummary owner;
  private String title;
  private String description;
  private Instant updatedAt;
  private long subscriberCount;
  private boolean subscribedByMe;
  private List<ContentSummary> contents;
}
