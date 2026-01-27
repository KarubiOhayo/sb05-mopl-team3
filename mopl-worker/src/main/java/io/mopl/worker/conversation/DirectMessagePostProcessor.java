package io.mopl.worker.conversation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.mopl.core.error.BusinessException;
import io.mopl.core.event.conversation.DirectMessageCreatedEvent;
import io.mopl.core.event.dm.DirectMessageReceivedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.common.WorkerErrorCode;
import io.mopl.worker.conversation.domain.DirectMessageRepository;
import io.mopl.worker.conversation.event.DirectMessageSavedEvent;
import io.mopl.worker.s3.S3PresignedUrlService;
import io.mopl.worker.user.domain.User;
import io.mopl.worker.user.domain.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final MeterRegistry meterRegistry;

  @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handleSavedEvent(DirectMessageSavedEvent event) {
    log.debug("DM 저장 후처리 시작 (Kafka 발행): dmId={}", event.dmId());

    List<User> users = userRepository.findAllById(List.of(event.senderId(), event.receiverId()));
    Map<UUID, User> userMap =
        users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    User sender = userMap.get(event.senderId());
    User receiver = userMap.get(event.receiverId());
    if (sender == null || receiver == null) {
      throw new BusinessException(WorkerErrorCode.USER_NOT_FOUND);
    }

    String senderProfileUrl =
        s3PresignedUrlService.generatePresignedUrl(sender.getProfileImageKey());
    String receiverProfileUrl =
        s3PresignedUrlService.generatePresignedUrl(receiver.getProfileImageKey());

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

    // 알림용 DM 수신 이벤트를 함께 발행한다.
    DirectMessageReceivedEvent receivedEvent =
        new DirectMessageReceivedEvent(
            event.dmId().toString(),
            event.createdAt(),
            event.conversationId().toString(),
            event.senderId().toString(),
            sender.getName(),
            event.receiverId().toString(),
            event.content());

    kafkaTemplate.send(
        KafkaTopics.DIRECT_MESSAGE_RECEIVED, event.receiverId().toString(), receivedEvent);

    // 상태 업데이트 (SENT)
    int updated = directMessageRepository.updateStatusToSentIfPending(event.dmId());
    if (updated == 0) {
      log.debug("DM 상태 업데이트 스킵: dmId={}, reason=not_pending_or_missing", event.dmId());
    }

    recordEndToEndDuration(event, "success");
    log.debug("DM 후처리 완료 (SENT): dmId={}", event.dmId());
  }

  @Recover
  public void recover(Exception e, DirectMessageSavedEvent event) {
    recordEndToEndDuration(event, "failed");
    log.error("DM 후처리 최종 실패 (상태 PENDING 유지): dmId={}", event.dmId(), e);
    // 추후 PENDING 상태의 메시지를 재처리하는 배치(Batch)가 필요함.
  }

  private void recordEndToEndDuration(DirectMessageSavedEvent event, String status) {
    Instant occurredAt = event.occurredAt();
    if (occurredAt == null) {
      return;
    }
    long nanos = Math.max(0L, java.time.Duration.between(occurredAt, Instant.now()).toNanos());
    Timer.builder("worker.dm.end_to_end.duration")
        .tags("status", status)
        .publishPercentileHistogram()
        .register(meterRegistry)
        .record(nanos, TimeUnit.NANOSECONDS);
  }
}
