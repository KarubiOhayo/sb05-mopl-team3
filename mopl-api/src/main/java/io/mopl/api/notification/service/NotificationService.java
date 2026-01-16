package io.mopl.api.notification.service;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.notification.domain.Notification;
import io.mopl.api.notification.domain.NotificationQueryRepository;
import io.mopl.api.notification.domain.NotificationRepository;
import io.mopl.api.notification.dto.NotificationDto;
import io.mopl.api.notification.dto.NotificationSearchRequest;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationQueryRepository notificationQueryRepository;

  @Transactional(readOnly = true)
  public CursorResponse<NotificationDto> findUnread(
      UUID userId, NotificationSearchRequest request) {
    if (userId == null) {
      throw new BusinessException(UserErrorCode.UNAUTHORIZED);
    }

    // QueryDSL 리포지토리에서 미읽음 알림을 커서 페이징으로 조회
    var page = notificationQueryRepository.findUnreadPage(userId, request);
    List<Notification> notifications = page.getNotifications();

    List<NotificationDto> data =
        notifications.stream()
            .map(
                notification ->
                    NotificationDto.builder()
                        .id(notification.getId())
                        .createdAt(notification.getCreatedAt())
                        .receiverId(notification.getReceiverId())
                        .title(notification.getTitle())
                        .content(notification.getContent())
                        .level(notification.getLevel())
                        .build())
            .toList();

    // 전체 미읽음 개수를 별도로 계산해 응답에 포함
    long totalCount = notificationQueryRepository.countUnread(userId);

    return CursorResponse.<NotificationDto>builder()
        .data(data)
        .nextCursor(page.getNextCursor())
        .nextIdAfter(page.getNextIdAfter())
        .hasNext(page.isHasNext())
        .totalCount(totalCount)
        .sortBy(request.sortBy())
        .sortDirection(request.sortDirection())
        .build();
  }

  @Transactional
  public void delete(UUID userId, UUID notificationId) {
    if (userId == null || notificationId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    // 수신자 본인의 알림만 삭제
    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));

    if (!notification.getReceiverId().equals(userId)) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    notificationRepository.deleteById(notificationId);
  }
}
