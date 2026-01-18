package io.mopl.core.event.conversation;

import java.time.Instant;

public record DirectMessageSendEvent(
    String eventId, Instant occurredAt, String conversationId, String senderId, String content) {}
