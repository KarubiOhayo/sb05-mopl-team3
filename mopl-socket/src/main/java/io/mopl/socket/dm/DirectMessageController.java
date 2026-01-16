package io.mopl.socket.dm;

import io.mopl.core.error.BusinessException;
import io.mopl.core.event.conversation.DirectMessageSendEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.socket.common.error.SocketErrorCode;
import io.mopl.socket.dm.dto.DirectMessageSendRequest;
import io.mopl.socket.websocket.security.SocketUserPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DirectMessageController {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @MessageMapping("/conversations/{conversationId}/direct-messages")
  public void sendDirectMessage(
      @DestinationVariable String conversationId,
      @Payload @Valid DirectMessageSendRequest request,
      Principal principal) {

    SocketUserPrincipal user = resolvePrincipal(principal);

    DirectMessageSendEvent event =
        new DirectMessageSendEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            conversationId,
            user.userId().toString(),
            request.content());

    kafkaTemplate.send(KafkaTopics.DIRECT_MESSAGE_SEND_REQUEST, conversationId, event);

    log.info("DM 전송 요청 발행 완료: conversationId={}, senderId={}", conversationId, user.userId());
  }

  private SocketUserPrincipal resolvePrincipal(Principal principal) {
    if (principal instanceof UsernamePasswordAuthenticationToken auth
        && auth.getPrincipal() instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }
    if (principal instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }

    throw new BusinessException(SocketErrorCode.MISSING_AUTHENTICATION);
  }
}
