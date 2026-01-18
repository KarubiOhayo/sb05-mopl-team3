package io.mopl.core.event.conversation;

import java.time.Instant;

public record DirectMessageCreatedEvent(
    String id,
    String conversationId,
    String senderId,
    String senderName,
    String senderProfileUrl,
    String receiverId,
    String receiverName,
    String receiverProfileUrl,
    String content,
    Instant createdAt) {}
