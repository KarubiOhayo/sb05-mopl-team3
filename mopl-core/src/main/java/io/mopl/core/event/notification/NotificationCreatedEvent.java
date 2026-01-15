package io.mopl.core.event.notification;

import java.time.Instant;

public record NotificationCreatedEvent(
    String eventId,
    Instant occurredAt,
    String notificationId,
    String receiverId,
    String title,
    String content,
    String level) {}
