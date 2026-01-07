package io.mopl.socket.websocket.security;

import java.security.Principal;
import java.util.UUID;

public record SocketUserPrincipal(UUID userId, String email, String role) implements Principal {

  @Override
  public String getName() {
    return userId.toString();
  }
}
