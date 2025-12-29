package io.mopl.api.notification.dto;

import io.mopl.api.notification.domain.NotificationLevel;
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
public class NotificationDto {

  private UUID id;
  private Instant createdAt;
  private UUID receiverId;
  private String title;
  private String content;
  private NotificationLevel level;
}
