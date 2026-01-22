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
import io.mopl.redis.constants.RedisKeyPrefix;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private static final Duration UNREAD_COUNT_TTL = Duration.ofHours(1);

  private final NotificationRepository notificationRepository;
  private final NotificationQueryRepository notificationQueryRepository;
  private final RedisTemplate<String, String> redisTemplate;

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
    Long cachedCount = getUnreadCountFromCache(userId);
    long totalCount =
        cachedCount != null ? cachedCount : notificationQueryRepository.countUnread(userId);
    if (cachedCount == null) {
      setUnreadCountCache(userId, totalCount);
    }

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
    if (notification.getReadAt() == null) {
      decrementUnreadCount(userId);
    }
  }

  private Long getUnreadCountFromCache(UUID userId) {
    String key = unreadCountKey(userId);
    try {
      String value = redisTemplate.opsForValue().get(key);
      if (value == null) {
        return null;
      }
      return Long.parseLong(value);
    } catch (Exception e) {
      redisTemplate.delete(key);
      return null;
    }
  }

  private void setUnreadCountCache(UUID userId, long count) {
    String key = unreadCountKey(userId);
    try {
      redisTemplate.opsForValue().set(key, String.valueOf(Math.max(count, 0)), UNREAD_COUNT_TTL);
    } catch (Exception e) {
      // 캐시 실패는 무시한다.
    }
  }

  private void decrementUnreadCount(UUID userId) {
    String key = unreadCountKey(userId);
    try {
      String current = redisTemplate.opsForValue().get(key);
      if (current == null) {
        return;
      }
      Long value = redisTemplate.opsForValue().increment(key, -1);
      if (value != null && value < 0) {
        redisTemplate.opsForValue().set(key, "0");
      }
    } catch (Exception e) {
      // 캐시 실패는 무시한다.
    }
  }

  private String unreadCountKey(UUID userId) {
    return RedisKeyPrefix.NOTIFICATION_UNREAD_COUNT + userId;
  }
}
