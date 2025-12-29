package io.mopl.api.conversation.dto;

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
public class DirectMessageDto {

  private UUID id;
  private UUID conversationId;
  private Instant createdAt;
  private UserSummary sender;
  private UserSummary receiver;
  private String content;
}
