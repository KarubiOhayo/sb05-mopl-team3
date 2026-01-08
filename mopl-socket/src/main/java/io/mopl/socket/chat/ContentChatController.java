package io.mopl.socket.chat;

import io.mopl.socket.chat.dto.ContentChatDto;
import io.mopl.socket.chat.dto.ContentChatSendRequest;
import io.mopl.socket.user.dto.UserSummary;
import io.mopl.socket.websocket.security.SocketUserPrincipal;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ContentChatController {

  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/contents/{contentId}/chat")
  public void sendChat(
      @DestinationVariable String contentId,
      @Payload ContentChatSendRequest request,
      Principal principal) {
    SocketUserPrincipal socketUser = resolvePrincipal(principal);

    UserSummary sender =
        UserSummary.builder()
            .userId(socketUser.userId())
            .name(socketUser.name())
            .profileImageUrl(socketUser.profileImageUrl())
            .build();

    ContentChatDto payload =
        ContentChatDto.builder().sender(sender).content(request.content()).build();

    messagingTemplate.convertAndSend("/sub/contents/" + contentId + "/chat", payload);
  }

  private SocketUserPrincipal resolvePrincipal(Principal principal) {
    if (principal instanceof UsernamePasswordAuthenticationToken auth
        && auth.getPrincipal() instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }
    if (principal instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }
    throw new IllegalStateException("WebSocket 인증 정보가 없습니다");
  }
}
