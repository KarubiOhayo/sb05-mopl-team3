package io.mopl.worker.conversation.event;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageSavedEvent(
    UUID dmId,
    UUID conversationId,
    UUID senderId,
    UUID receiverId,
    String content,
    Instant createdAt,
    Instant occurredAt) {}
