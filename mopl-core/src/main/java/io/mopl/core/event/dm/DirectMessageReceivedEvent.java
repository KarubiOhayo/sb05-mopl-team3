package io.mopl.core.event.dm;

import java.time.Instant;

public record DirectMessageReceivedEvent(
    String eventId,
    Instant occurredAt,
    String conversationId,
    String senderId,
    String senderName,
    String receiverId,
    String content) {}
