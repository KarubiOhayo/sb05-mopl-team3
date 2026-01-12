package io.mopl.socket.chat.dto;

import io.mopl.socket.user.dto.UserSummary;
import lombok.Builder;

@Builder
public record ContentChatDto(UserSummary sender, String content) {}
