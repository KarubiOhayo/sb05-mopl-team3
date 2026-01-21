package io.mopl.worker.notification;

import io.mopl.core.db.DbConstraintNames;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationListenerSupport {

  static void handleDataIntegrityViolation(
      Logger log, DataIntegrityViolationException e, String eventId) {
    Throwable cause = e.getMostSpecificCause();
    String message = cause != null ? cause.getMessage() : e.getMessage();

    if (message != null && message.contains(DbConstraintNames.UQ_NOTIFICATIONS_EVENT_ID)) {
      log.debug("중복 이벤트 무시 (eventId={})", eventId);
    } else if (message != null && message.contains(DbConstraintNames.FK_NOTIFICATIONS_RECEIVER)) {
      log.error("수신자 참조 오류 (eventId={})", eventId, e);
    } else {
      log.error("알림 저장 중 오류 (eventId={})", eventId, e);
    }
  }

  static UUID parseUuid(Logger log, String value, String fieldName, String eventId) {
    try {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("value is null/blank");
      }
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      log.error("UUID 형식이 올바르지 않습니다: {} (eventId={})", fieldName, eventId, e);
      throw e;
    }
  }
}
