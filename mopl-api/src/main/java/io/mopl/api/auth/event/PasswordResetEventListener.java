package io.mopl.api.auth.event;

import io.mopl.api.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 비밀번호 초기화 이벤트 리스너 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetEventListener {

  private final EmailService emailService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePasswordResetEvent(PasswordResetEvent event) {
    try {
      emailService.sendTemporaryPassword(event.getEmail(), event.getTemporaryPassword());
    } catch (Exception e) {
      log.error("임시 비밀번호 이메일 전송 실패: userId = {}", event.getUserId(), e);
    }
  }
}
