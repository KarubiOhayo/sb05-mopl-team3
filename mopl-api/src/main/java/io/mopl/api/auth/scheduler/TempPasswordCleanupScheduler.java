package io.mopl.api.auth.scheduler;

import io.mopl.api.user.domain.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TempPasswordCleanupScheduler {

  private final UserRepository userRepository;

  /** 만료된 임시 비밀번호 자동 정리, 매 1분마다 */
  @Scheduled(cron = "0 * * * * *")
  @Transactional
  public void cleanupExpiredTempPasswords() {
    Instant now = Instant.now();
    int count = userRepository.clearExpiredTempPasswords(now);

    if (count > 0) {
      log.info("만료된 임시 비밀번호 자동 정리: {}건", count);
    }
  }
}
