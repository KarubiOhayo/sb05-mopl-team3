package io.mopl.socket.websocket.security;

import io.mopl.socket.auth.jwt.JwtTokenProvider;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
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

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

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
              jwtTokenProvider.getRole(token));
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
      accessor.setUser(authentication);
    }

    return message;
  }

  private String resolveToken(StompHeaderAccessor accessor) {
    List<String> authorization = accessor.getNativeHeader(AUTHORIZATION);
    if (authorization != null && !authorization.isEmpty()) {
      String value = authorization.get(0);
      if (StringUtils.hasText(value) && value.startsWith(BEARER_PREFIX)) {
        return value.substring(BEARER_PREFIX.length());
      }
      return value;
    }

    List<String> accessToken = accessor.getNativeHeader(ACCESS_TOKEN);
    if (accessToken != null && !accessToken.isEmpty()) {
      return accessToken.get(0);
    }

    return null;
  }
}
