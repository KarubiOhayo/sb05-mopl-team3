package io.mopl.socket.websocket.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SessionSubscriptionRegistry {

  private final Map<String, String> subscriptions = new ConcurrentHashMap<>();

  public void register(String sessionId, String subscriptionId, String destination) {
    subscriptions.put(key(sessionId, subscriptionId), destination);
  }

  public String unregister(String sessionId, String subscriptionId) {
    return subscriptions.remove(key(sessionId, subscriptionId));
  }

  public void removeSession(String sessionId) {
    subscriptions.keySet().removeIf(key -> key.startsWith(sessionId + ":"));
  }

  private String key(String sessionId, String subscriptionId) {
    return sessionId + ":" + subscriptionId;
  }
}
