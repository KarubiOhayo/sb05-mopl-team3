package io.mopl.socket.watching.dto;

import io.mopl.socket.content.dto.ContentSummary;
import io.mopl.socket.user.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionDto(
    UUID id, Instant createdAt, UserSummary watcher, ContentSummary content) {}
