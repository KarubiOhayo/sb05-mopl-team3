package io.mopl.socket.chat.dto;

import lombok.Builder;

@Builder
public record ContentChatSendRequest(String content) {}
