package io.mopl.api.notification.dto;

import io.mopl.api.notification.domain.Notification;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationPage {
  private final List<Notification> notifications;
  private final boolean hasNext;
  private final String nextCursor;
  private final UUID nextIdAfter;
}
