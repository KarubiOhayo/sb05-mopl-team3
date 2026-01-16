package io.mopl.worker.conversation;

import io.mopl.core.error.BusinessException;
import io.mopl.core.event.conversation.DirectMessageCreatedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.common.WorkerErrorCode;
import io.mopl.worker.conversation.domain.DirectMessage;
import io.mopl.worker.conversation.domain.DirectMessageRepository;
import io.mopl.worker.conversation.event.DirectMessageSavedEvent;
import io.mopl.worker.s3.S3PresignedUrlService;
import io.mopl.worker.user.domain.User;
import io.mopl.worker.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessagePostProcessor {

  private final UserRepository userRepository;
  private final S3PresignedUrlService s3PresignedUrlService;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final DirectMessageRepository directMessageRepository;

  @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleSavedEvent(DirectMessageSavedEvent event) {
    log.info("DM 저장 후처리 시작 (Kafka 발행): dmId={}", event.dmId());

    User sender =
        userRepository
            .findById(event.senderId())
            .orElseThrow(() -> new BusinessException(WorkerErrorCode.USER_NOT_FOUND));

    User receiver =
        userRepository
            .findById(event.receiverId())
            .orElseThrow(() -> new BusinessException(WorkerErrorCode.USER_NOT_FOUND));

    String senderProfileUrl =
        s3PresignedUrlService.generatePresignedUrl(sender.getProfileImageUrl());
    String receiverProfileUrl =
        s3PresignedUrlService.generatePresignedUrl(receiver.getProfileImageUrl());

    DirectMessageCreatedEvent createdEvent =
        new DirectMessageCreatedEvent(
            event.dmId().toString(),
            event.conversationId().toString(),
            event.senderId().toString(),
            sender.getName(),
            senderProfileUrl,
            event.receiverId().toString(),
            receiver.getName(),
            receiverProfileUrl,
            event.content(),
            event.createdAt());

    kafkaTemplate.send(
        KafkaTopics.DIRECT_MESSAGE_CREATED, event.conversationId().toString(), createdEvent);

    // 상태 업데이트 (SENT)
    // 트랜잭션 종료 시 더티 체킹으로 업데이트됨
    directMessageRepository.findById(event.dmId()).ifPresent(DirectMessage::markAsSent);

    log.info("DM 후처리 완료 (SENT): dmId={}", event.dmId());
  }

  @Recover
  public void recover(Exception e, DirectMessageSavedEvent event) {
    log.error("DM 후처리 최종 실패 (상태 PENDING 유지): dmId={}", event.dmId(), e);
    // 추후 PENDING 상태의 메시지를 재처리하는 배치(Batch)가 필요함.
  }
}
