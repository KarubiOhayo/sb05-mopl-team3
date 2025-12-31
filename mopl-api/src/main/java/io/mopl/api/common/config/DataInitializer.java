package io.mopl.api.common.config;

import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final InitAccountProperties initAccountProperties;

  @Override
  public void run(String... args) throws Exception {
    initializeAdmin();
    initializeTestUsers();
  }

  /** 어드민 계정 초기화 이메일: admin@mopl.io 비밀번호: 1234 */
  private void initializeAdmin() {
    String adminEmail = initAccountProperties.getAdmin().getEmail();

    if (userRepository.existsByEmail(adminEmail)) {
      log.info("Admin 계정이 이미 존재합니다");
      return;
    }

    User admin =
        User.builder()
            .email(adminEmail)
            .name(initAccountProperties.getAdmin().getName())
            .passwordHash(passwordEncoder.encode(initAccountProperties.getAdmin().getPassword()))
            .role(UserRole.ADMIN)
            .authProvider(AuthProvider.LOCAL)
            .locked(false)
            .build();

    userRepository.save(admin);
    log.info("Admin 계정 생성 완료: userId={}", admin.getId());
  }

  /** 일반 사용자 계정 초기화 비밀번호: 1234 */
  private void initializeTestUsers() {
    String userPassword = initAccountProperties.getTestUserPassword();

    createUserIfNotExists("woody@mopl.io", "우디", userPassword);
    createUserIfNotExists("buzz@mopl.io", "버즈", userPassword);
    createUserIfNotExists("jessie@mopl.io", "제시", userPassword);
    createUserIfNotExists("rex@mopl.io", "렉스", userPassword);
    createUserIfNotExists("slinky@mopl.io", "슬링키", userPassword);
  }

  private void createUserIfNotExists(String email, String name, String password) {
    if (userRepository.existsByEmail(email)) {
      log.info("User 계정이 이미 존재합니다");
      return;
    }

    User user =
        User.builder()
            .email(email)
            .name(name)
            .passwordHash(passwordEncoder.encode(password))
            .role(UserRole.USER)
            .authProvider(AuthProvider.LOCAL)
            .locked(false)
            .build();

    userRepository.save(user);
    log.info("User 계정 생성 완료: {} userId={}", name, user.getId());
  }
}
