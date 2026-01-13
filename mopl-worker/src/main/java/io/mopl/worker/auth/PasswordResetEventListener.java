package io.mopl.worker.auth;

import io.mopl.core.event.auth.PasswordResetEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetEventListener {

  private final EmailService emailService;

  @KafkaListener(
      topics = KafkaTopics.AUTH_PASSWORD_RESET,
      properties = "spring.json.value.default.type=io.mopl.core.event.auth.PasswordResetEvent")
  public void handlePasswordResetEvent(PasswordResetEvent event, Acknowledgment ack) {
    try {
      emailService.sendTemporaryPassword(event.email(), event.temporaryPassword());
    } catch (Exception e) {
      log.error("임시 비밀번호 이메일 발송 실패: eventId = {}", event.eventId(), e);
    } finally {
      ack.acknowledge();
    }
  }
}
