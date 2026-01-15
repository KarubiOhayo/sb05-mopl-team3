package io.mopl.worker.conversation;

import io.mopl.core.error.BusinessException;
import io.mopl.core.event.conversation.DirectMessageCreatedEvent;
import io.mopl.core.event.conversation.DirectMessageSendEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.common.WorkerErrorCode;
import io.mopl.worker.conversation.domain.ConversationParticipant;
import io.mopl.worker.conversation.domain.ConversationParticipantRepository;
import io.mopl.worker.conversation.domain.DirectMessage;
import io.mopl.worker.conversation.domain.DirectMessageRepository;
import io.mopl.worker.s3.S3PresignedUrlService;
import io.mopl.worker.user.domain.User;
import io.mopl.worker.user.domain.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageListener {

  private final DirectMessageRepository directMessageRepository;
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final UserRepository userRepository;
  private final S3PresignedUrlService s3PresignedUrlService;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @KafkaListener(
      topics = KafkaTopics.DIRECT_MESSAGE_SEND_REQUEST,
      groupId = "mopl-worker-dm-group",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.conversation.DirectMessageSendEvent")
  @Transactional
  public void handleSendRequest(DirectMessageSendEvent event, Acknowledgment ack) {
    log.info("DM 전송 요청 수신: eventId={}, conversationId={}", event.eventId(), event.conversationId());

    try {
      UUID conversationId = UUID.fromString(event.conversationId());
      UUID senderId = UUID.fromString(event.senderId());

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
      User sender =
          userRepository
              .findById(senderId)
              .orElseThrow(() -> new BusinessException(WorkerErrorCode.USER_NOT_FOUND));
      User receiver =
          userRepository
              .findById(receiverId)
              .orElseThrow(() -> new BusinessException(WorkerErrorCode.USER_NOT_FOUND));
      String senderProfileUrl =
          s3PresignedUrlService.generatePresignedUrl(sender.getProfileImageUrl());
      String receiverProfileUrl =
          s3PresignedUrlService.generatePresignedUrl(receiver.getProfileImageUrl());

      DirectMessageCreatedEvent createdEvent =
          new DirectMessageCreatedEvent(
              savedDm.getId().toString(),
              savedDm.getConversationId().toString(),
              savedDm.getSenderId().toString(),
              sender.getName(),
              senderProfileUrl,
              savedDm.getReceiverId().toString(),
              receiver.getName(),
              receiverProfileUrl,
              savedDm.getContent(),
              savedDm.getCreatedAt());

      kafkaTemplate.send(
          KafkaTopics.DIRECT_MESSAGE_CREATED, savedDm.getConversationId().toString(), createdEvent);

      log.info("DM 생성 및 이벤트 발행 완료: dmId={}", savedDm.getId());

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
