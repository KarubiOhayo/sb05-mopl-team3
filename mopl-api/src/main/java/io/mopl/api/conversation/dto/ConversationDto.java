package io.mopl.api.conversation.dto;

import io.mopl.api.user.dto.UserSummary;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationDto(
    UUID id, UserSummary with, DirectMessageDto lastestMessage, boolean hasUnread) {}
