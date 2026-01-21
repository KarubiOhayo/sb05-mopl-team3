package io.mopl.socket.metrics;

import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
@RequiredArgsConstructor
public class WebSocketMetricsListener {

  private static final Pattern WATCH_PATTERN = Pattern.compile("^/sub/contents/[^/]+/watch$");
  private static final Pattern CHAT_PATTERN = Pattern.compile("^/sub/contents/[^/]+/chat$");
  private static final Pattern DM_PATTERN =
      Pattern.compile("^/sub/conversations/[^/]+/direct-messages$");

  private final SocketMetrics socketMetrics;

  @EventListener
  public void handleConnect(SessionConnectEvent event) {
    socketMetrics.onWsConnect();
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    socketMetrics.onWsDisconnect();
  }

  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();
    socketMetrics.onWsSubscribe(classifyDestination(destination));
  }

  @EventListener
  public void handleUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = accessor.getDestination();
    socketMetrics.onWsUnsubscribe(classifyDestination(destination));
  }

  private String classifyDestination(String destination) {
    if (!StringUtils.hasText(destination)) {
      return "unknown";
    }
    if (WATCH_PATTERN.matcher(destination).matches()) {
      return "watch";
    }
    if (CHAT_PATTERN.matcher(destination).matches()) {
      return "chat";
    }
    if (DM_PATTERN.matcher(destination).matches()) {
      return "dm";
    }
    return "other";
  }
}
