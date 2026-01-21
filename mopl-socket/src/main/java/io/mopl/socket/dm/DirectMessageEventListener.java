package io.mopl.socket.dm;

import io.mopl.core.event.conversation.DirectMessageCreatedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.socket.dm.dto.DirectMessageDto;
import io.mopl.socket.sse.SseService;
import io.mopl.socket.user.dto.UserSummary;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageEventListener {

  private final SimpMessagingTemplate messagingTemplate;
  private final SseService sseService;
  private final DirectMessageSubscriptionRegistry dmSubscriptionRegistry;

  @KafkaListener(
      topics = KafkaTopics.DIRECT_MESSAGE_CREATED,
      groupId = "mopl-socket-dm-group",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.conversation.DirectMessageCreatedEvent")
  @Transactional(readOnly = true)
  public void handleCreatedEvent(DirectMessageCreatedEvent event) {
    try {
      log.info("DM 생성 이벤트 수신: dmId={}", event.id());

      DirectMessageDto dto =
          DirectMessageDto.builder()
              .id(UUID.fromString(event.id()))
              .conversationId(UUID.fromString(event.conversationId()))
              .createdAt(event.createdAt())
              .content(event.content())
              .sender(
                  UserSummary.builder()
                      .userId(UUID.fromString(event.senderId()))
                      .name(event.senderName())
                      .profileImageUrl(event.senderProfileUrl())
                      .build())
              .receiver(
                  UserSummary.builder()
                      .userId(UUID.fromString(event.receiverId()))
                      .name(event.receiverName())
                      .profileImageUrl(event.receiverProfileUrl())
                      .build())
              .build();

      messagingTemplate.convertAndSend(
          "/sub/conversations/" + event.conversationId() + "/direct-messages", dto);

      // 해당 대화가 열려 있지 않을 때만 SSE로 보낸다.
      if (!dmSubscriptionRegistry.isUserSubscribed(event.receiverId(), event.conversationId())) {
        sseService.send(event.receiverId(), "direct-messages", dto);
      }

    } catch (Exception e) {
      log.error("DM 생성 이벤트 처리 중 오류 발생 (재시도/DLQ 예정)", e);
      throw e;
    }
  }
}
