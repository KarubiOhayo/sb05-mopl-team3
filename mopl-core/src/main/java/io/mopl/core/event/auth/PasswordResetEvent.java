package io.mopl.core.event.auth;

import java.time.Instant;
import java.util.UUID;

/** 비밀번호 초기화 이벤트 */
public record PasswordResetEvent(
    String eventId, String userId, String email, String temporaryPassword, Instant occurredAt) {

  public static PasswordResetEvent of(UUID userId, String email, String temporaryPassword) {
    return new PasswordResetEvent(
        UUID.randomUUID().toString(), userId.toString(), email, temporaryPassword, Instant.now());
  }
}
