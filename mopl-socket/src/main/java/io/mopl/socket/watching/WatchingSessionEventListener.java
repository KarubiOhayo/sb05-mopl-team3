package io.mopl.socket.watching;

import io.mopl.socket.content.dto.ContentSummary;
import io.mopl.socket.user.dto.UserSummary;
import io.mopl.socket.watching.dto.ChangeType;
import io.mopl.socket.watching.dto.WatchingSessionChange;
import io.mopl.socket.watching.dto.WatchingSessionDto;
import io.mopl.socket.websocket.security.SocketUserPrincipal;
import io.mopl.socket.websocket.session.SessionSubscriptionRegistry;
import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionEventListener {

  private static final Pattern WATCH_DESTINATION_PATTERN =
      Pattern.compile("^/sub/contents/([^/]+)/watch$");

  private final WatchingSessionService watchingSessionService;
  private final SimpMessagingTemplate messagingTemplate;
  private final SessionSubscriptionRegistry subscriptionRegistry;

  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();
    String sessionId = accessor.getSessionId();
    String subscriptionId = accessor.getSubscriptionId();

    if (StringUtils.hasText(sessionId)
        && StringUtils.hasText(subscriptionId)
        && StringUtils.hasText(destination)) {
      subscriptionRegistry.register(sessionId, subscriptionId, destination);
    }

    String contentId = extractContentId(destination);
    if (contentId == null) {
      return;
    }

    SocketUserPrincipal socketUser = resolvePrincipal(event.getUser());
    if (socketUser == null) {
      log.warn("사용자 인증 정보가 없어 구독 이벤트가 무시되었습니다. destination={}", destination);
      return;
    }

    // 이름과 프로필 이미지도 함께 저장
    long watcherCount =
        watchingSessionService.join(
            contentId, socketUser.userId(), socketUser.name(), socketUser.profileImageUrl());

    WatchingSessionChange change =
        WatchingSessionChange.builder()
            .type(ChangeType.JOIN)
            .watchingSession(buildSession(contentId, socketUser))
            .watcherCount(watcherCount)
            .build();

    messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/watch", change);
  }

  @EventListener
  public void handleUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = accessor.getSessionId();
    String subscriptionId = accessor.getSubscriptionId();

    if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(subscriptionId)) {
      return;
    }

    String destination = subscriptionRegistry.unregister(sessionId, subscriptionId);
    String contentId = extractContentId(destination);
    if (contentId == null) {
      return;
    }

    SocketUserPrincipal socketUser = resolvePrincipal(event.getUser());
    if (socketUser == null) {
      return;
    }

    long watcherCount = watchingSessionService.leave(contentId, socketUser.userId());

    WatchingSessionChange change =
        WatchingSessionChange.builder()
            .type(ChangeType.LEAVE)
            .watchingSession(buildSession(contentId, socketUser))
            .watcherCount(watcherCount)
            .build();

    messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/watch", change);
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    Principal principal = event.getUser();
    if (principal == null) {
      return;
    }

    SocketUserPrincipal socketUser = resolvePrincipal(principal);
    if (socketUser == null) {
      return;
    }

    Optional<String> contentId = watchingSessionService.getWatchingContentId(socketUser.userId());
    if (contentId.isEmpty()) {
      subscriptionRegistry.removeSession(event.getSessionId());
      return;
    }

    long watcherCount = watchingSessionService.leave(contentId.get(), socketUser.userId());

    WatchingSessionChange change =
        WatchingSessionChange.builder()
            .type(ChangeType.LEAVE)
            .watchingSession(buildSession(contentId.get(), socketUser))
            .watcherCount(watcherCount)
            .build();

    messagingTemplate.convertAndSend("/sub/contents/" + contentId.get() + "/watch", change);
    subscriptionRegistry.removeSession(event.getSessionId());
  }

  private String extractContentId(String destination) {
    if (!StringUtils.hasText(destination)) {
      return null;
    }
    Matcher matcher = WATCH_DESTINATION_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return null;
    }
    return matcher.group(1);
  }

  private SocketUserPrincipal resolvePrincipal(Principal principal) {
    if (principal == null) {
      return null;
    }
    if (principal instanceof UsernamePasswordAuthenticationToken auth
        && auth.getPrincipal() instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }
    if (principal instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }
    // log.warn("Unknown principal type: {}", principal.getClass().getName()); // 인증되지 않은 세션일 수 있으므로
    // 로그 레벨 조정 또는 제거
    return null;
  }

  private WatchingSessionDto buildSession(String contentId, SocketUserPrincipal socketUser) {
    UserSummary watcher =
        UserSummary.builder()
            .userId(socketUser.userId())
            .name(socketUser.name())
            .profileImageUrl(socketUser.profileImageUrl())
            .build();

    ContentSummary content = ContentSummary.builder().id(UUID.fromString(contentId)).build();

    return WatchingSessionDto.builder()
        // 세션 ID를 랜덤 UUID 대신 userId로 고정하여 프론트엔드 상태 동기화 문제 해결
        .id(socketUser.userId())
        .createdAt(Instant.now())
        .watcher(watcher)
        .content(content)
        .build();
  }
}
