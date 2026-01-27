package io.mopl.worker.conversation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.mopl.core.error.BusinessException;
import io.mopl.core.event.conversation.DirectMessageSendEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.common.WorkerErrorCode;
import io.mopl.worker.conversation.domain.ConversationParticipant;
import io.mopl.worker.conversation.domain.ConversationParticipantRepository;
import io.mopl.worker.conversation.domain.DirectMessage;
import io.mopl.worker.conversation.domain.DirectMessageRepository;
import io.mopl.worker.conversation.event.DirectMessageSavedEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageListener {

  private final DirectMessageRepository directMessageRepository;
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final MeterRegistry meterRegistry;

  @KafkaListener(
      topics = KafkaTopics.DIRECT_MESSAGE_SEND_REQUEST,
      groupId = "mopl-worker-dm-group",
      properties =
          "spring.json.value.default.type=io.mopl.core.event.conversation.DirectMessageSendEvent")
  @Transactional
  public void handleSendRequest(DirectMessageSendEvent event) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String status = "success";
    log.debug(
        "DM 전송 요청 수신: eventId={}, conversationId={}, senderId={}",
        event.eventId(),
        event.conversationId(),
        event.senderId());

    try {
      UUID conversationId = UUID.fromString(event.conversationId());
      UUID senderId = UUID.fromString(event.senderId());

      // 1. 참여자 조회 및 권한 검증
      List<ConversationParticipant> participants =
          conversationParticipantRepository.findAllByConversationId(conversationId);
      boolean senderExists = false;
      UUID receiverId = null;
      for (ConversationParticipant participant : participants) {
        UUID userId = participant.getId().getUserId();
        if (senderId.equals(userId)) {
          senderExists = true;
        } else if (receiverId == null) {
          receiverId = userId;
        }
      }
      if (!senderExists) {
        log.error(
            "DM 전송 권한 없음: 사용자가 대화 참여자가 아님. userId={}, conversationId={}", senderId, conversationId);
        throw new BusinessException(WorkerErrorCode.NOT_A_CONVERSATION_PARTICIPANT);
      }
      if (receiverId == null) {
        throw new BusinessException(WorkerErrorCode.DM_RECEIVER_NOT_FOUND);
      }

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
              savedDm.getCreatedAt(),
              event.occurredAt()));

      log.debug("DM 저장 완료 (PENDING): dmId={}", savedDm.getId());

    } catch (IllegalArgumentException e) {
      status = "invalid_request";
      log.error("DM 전송 요청 데이터가 유효하지 않음 (DLQ로 이동): {}", e.getMessage());
      throw e; // GlobalErrorHandler가 DLQ로 보냄
    } catch (BusinessException e) {
      status = "business_error";
      log.error("비즈니스 로직 오류 발생 (DLQ로 이동): {}", e.getErrorCode());
      throw e; // GlobalErrorHandler가 DLQ로 보냄
    } catch (Exception e) {
      status = "failed";
      log.error("DM 전송 요청 처리 중 예외 발생 (재시도 수행): {}", e.getMessage(), e);
      throw e; // GlobalErrorHandler가 재시도 수행
    } finally {
      sample.stop(
          Timer.builder("worker.dm.handle.duration")
              .tags("status", status)
              .publishPercentileHistogram()
              .register(meterRegistry));
    }
  }
}
