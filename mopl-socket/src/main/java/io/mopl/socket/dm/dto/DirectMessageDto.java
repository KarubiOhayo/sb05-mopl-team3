package io.mopl.socket.dm.dto;

import io.mopl.socket.user.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    Instant createdAt,
    UserSummary sender,
    UserSummary receiver,
    String content) {}
