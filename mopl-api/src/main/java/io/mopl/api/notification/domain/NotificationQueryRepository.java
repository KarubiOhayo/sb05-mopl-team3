package io.mopl.api.notification.domain;

import io.mopl.api.notification.dto.NotificationPage;
import io.mopl.api.notification.dto.NotificationSearchRequest;
import java.util.UUID;

public interface NotificationQueryRepository {

  NotificationPage findUnreadPage(UUID receiverId, NotificationSearchRequest request);

  long countUnread(UUID receiverId);
}
