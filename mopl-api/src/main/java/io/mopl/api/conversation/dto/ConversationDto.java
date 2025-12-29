package io.mopl.api.conversation.dto;

import io.mopl.api.user.dto.UserSummary;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDto {

  private UUID id;
  private UserSummary with;
  private DirectMessageDto lastestMessage;
  private boolean hasUnread;
}
