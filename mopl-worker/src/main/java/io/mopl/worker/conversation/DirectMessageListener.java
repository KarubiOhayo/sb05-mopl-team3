package io.mopl.worker.conversation;

import io.mopl.core.error.BusinessException;
import io.mopl.core.event.conversation.DirectMessageSendEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.common.WorkerErrorCode;
import io.mopl.worker.conversation.domain.ConversationParticipant;
import io.mopl.worker.conversation.domain.ConversationParticipantId;
import io.mopl.worker.conversation.domain.ConversationParticipantRepository;
import io.mopl.worker.conversation.domain.DirectMessage;
import io.mopl.worker.conversation.domain.DirectMessageRepository;
import io.mopl.worker.conversation.event.DirectMessageSavedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageListener {

  private final DirectMessageRepository directMessageRepository;
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  @KafkaListener(
      topics = KafkaTopics.DIRECT_MESSAGE_SEND_REQUEST,
      groupId = "mopl-worker-dm-group",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.conversation.DirectMessageSendEvent")
  @Transactional
  public void handleSendRequest(DirectMessageSendEvent event, Acknowledgment ack) {
    log.info(
        "DM 전송 요청 수신: eventId={}, conversationId={}, senderId={}",
        event.eventId(),
        event.conversationId(),
        event.senderId());

    try {
      UUID conversationId = UUID.fromString(event.conversationId());
      UUID senderId = UUID.fromString(event.senderId());

      // 1. 송신자가 해당 대화의 참여자인지 검증
      if (!conversationParticipantRepository.existsById(
          new ConversationParticipantId(conversationId, senderId))) {
        log.error(
            "DM 전송 권한 없음: 사용자가 대화 참여자가 아님. userId={}, conversationId={}", senderId, conversationId);
        throw new BusinessException(WorkerErrorCode.NOT_A_CONVERSATION_PARTICIPANT);
      }

      // 2. 수신자 조회
      ConversationParticipant receiverParticipant =
          conversationParticipantRepository
              .findReceiver(conversationId, senderId)
              .orElseThrow(() -> new BusinessException(WorkerErrorCode.DM_RECEIVER_NOT_FOUND));

      UUID receiverId = receiverParticipant.getId().getUserId();

      DirectMessage dm =
          DirectMessage.builder()
              .conversationId(conversationId)
              .senderId(senderId)
              .receiverId(receiverId)
              .content(event.content())
              .build();

      DirectMessage savedDm = directMessageRepository.save(dm);

      // 3. 내부 이벤트 발행 (트랜잭션 커밋 후 Kafka 전송 처리)
      applicationEventPublisher.publishEvent(
          new DirectMessageSavedEvent(
              savedDm.getId(),
              savedDm.getConversationId(),
              savedDm.getSenderId(),
              savedDm.getReceiverId(),
              savedDm.getContent(),
              savedDm.getCreatedAt()));

      log.info("DM 저장 완료 (PENDING): dmId={}", savedDm.getId());

      // 처리가 성공했을 때만 ACK
      ack.acknowledge();
    } catch (IllegalArgumentException e) {
      log.error("DM 전송 요청 데이터가 유효하지 않음 (DLQ로 이동): {}", e.getMessage());
      throw e; // GlobalErrorHandler가 DLQ로 보냄
    } catch (BusinessException e) {
      log.error("비즈니스 로직 오류 발생 (DLQ로 이동): {}", e.getErrorCode());
      throw e; // GlobalErrorHandler가 DLQ로 보냄
    } catch (Exception e) {
      log.error("DM 전송 요청 처리 중 예외 발생 (재시도 수행): {}", e.getMessage(), e);
      throw e; // GlobalErrorHandler가 재시도 수행
    }
  }
}
