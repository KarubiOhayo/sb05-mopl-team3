package io.mopl.worker.notification;

import io.mopl.core.db.DbConstraintNames;
import io.mopl.worker.notification.domain.Notification;
import io.mopl.worker.notification.domain.NotificationRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationListenerSupport {

  static String classifyDataIntegrityViolation(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();
    String message = cause != null ? cause.getMessage() : e.getMessage();

    if (message != null && message.contains(DbConstraintNames.UQ_NOTIFICATIONS_EVENT_ID)) {
      return "duplicate";
    }
    if (message != null && message.contains(DbConstraintNames.FK_NOTIFICATIONS_RECEIVER)) {
      return "invalid_receiver";
    }
    return "data_integrity";
  }

  static void handleDataIntegrityViolation(
      Logger log, DataIntegrityViolationException e, String eventId) {
    String reason = classifyDataIntegrityViolation(e);
    if ("duplicate".equals(reason)) {
      log.debug("중복 이벤트 무시 (eventId={})", eventId);
      return;
    }
    if ("invalid_receiver".equals(reason)) {
      log.error("수신자 참조 오류 (eventId={})", eventId, e);
      return;
    }
    log.error("알림 저장 중 오류 (eventId={})", eventId, e);
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

  static void saveAndPublishBatch(
      Logger log,
      List<Notification> notifications,
      String eventId,
      NotificationRepository notificationRepository,
      NotificationEventPublisher notificationEventPublisher,
      NotificationMetrics notificationMetrics,
      String type) {
    if (notifications.isEmpty()) {
      return;
    }

    try {
      List<Notification> saved = notificationRepository.saveAll(notifications);
      saved.forEach(notificationEventPublisher::publish);
    } catch (DataIntegrityViolationException ex) {
      for (Notification notification : notifications) {
        try {
          Notification saved = notificationRepository.save(notification);
          notificationEventPublisher.publish(saved);
        } catch (DataIntegrityViolationException inner) {
          notificationMetrics.recordFailure(type, classifyDataIntegrityViolation(inner));
          NotificationListenerSupport.handleDataIntegrityViolation(
              log, inner, eventId + ":" + notification.getReceiverId());
        }
      }
    }
  }

  static UUID toPerReceiverEventId(String eventId, UUID receiverId) {
    String source = eventId + ":" + receiverId;
    return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
  }
}
