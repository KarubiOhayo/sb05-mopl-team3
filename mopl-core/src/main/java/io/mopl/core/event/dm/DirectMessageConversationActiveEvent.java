package io.mopl.core.event.dm;

import java.time.Instant;

// DM 대화 활성 상태를 알리기 위한 이벤트
public record DirectMessageConversationActiveEvent(
    String eventId, Instant occurredAt, String userId, String conversationId, boolean active) {}
