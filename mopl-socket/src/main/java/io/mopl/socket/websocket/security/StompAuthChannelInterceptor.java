package io.mopl.socket.websocket.security;

import io.mopl.socket.auth.jwt.JwtTokenProvider;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private static final String AUTHORIZATION = "Authorization";
  private static final String ACCESS_TOKEN = "accessToken";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String USER_SESSION_ATTRIBUTE = "USER_PRINCIPAL";

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      String token = resolveToken(accessor);
      if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
        log.warn("WebSocket CONNECT 인증 실패");
        throw new IllegalArgumentException("Invalid WebSocket token");
      }

      SocketUserPrincipal principal =
          new SocketUserPrincipal(
              jwtTokenProvider.getUserId(token),
              jwtTokenProvider.getEmail(token),
              jwtTokenProvider.getRole(token),
              jwtTokenProvider.getName(token),
              jwtTokenProvider.getProfileImageUrl(token));
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());

      accessor.setUser(authentication);

      // 세션 속성에 저장 (영속성 보장)
      Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
      if (sessionAttributes != null) {
        sessionAttributes.put(USER_SESSION_ATTRIBUTE, authentication);
      }
    } else {
      // CONNECT가 아닌 경우: User가 없으면 세션에서 복구
      if (accessor.getUser() == null) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
          Principal principal = (Principal) sessionAttributes.get(USER_SESSION_ATTRIBUTE);
          if (principal != null) {
            accessor.setUser(principal);
          }
        }
      }
    }

    return message;
  }

  private String resolveToken(StompHeaderAccessor accessor) {
    List<String> authorization = accessor.getNativeHeader(AUTHORIZATION);
    if (authorization != null && !authorization.isEmpty()) {
      String value = authorization.getFirst();
      if (StringUtils.hasText(value) && value.startsWith(BEARER_PREFIX)) {
        return value.substring(BEARER_PREFIX.length());
      }
      return value;
    }

    List<String> accessToken = accessor.getNativeHeader(ACCESS_TOKEN);
    if (accessToken != null && !accessToken.isEmpty()) {
      return accessToken.getFirst();
    }

    return null;
  }
}
