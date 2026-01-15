package io.mopl.core.event.notification;

import java.time.Instant;

/**
 * 알림 저장 결과를 외부로 알리기 위한 이벤트.
 *
 * <p>eventId는 소비 중복 방지를 위한 식별자이며, 현재는 알림 ID와 동일하게 사용한다.
 */
public record NotificationCreatedEvent(
    String eventId,
    Instant occurredAt,
    String notificationId,
    String receiverId,
    String title,
    String content,
    String level) {}
