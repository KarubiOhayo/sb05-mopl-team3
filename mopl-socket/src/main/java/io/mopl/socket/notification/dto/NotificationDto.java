package io.mopl.socket.notification.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

// SSE 알림 전송용 DTO
@Builder
public record NotificationDto(
    UUID id, Instant createdAt, UUID receiverId, String title, String content, String level) {}
