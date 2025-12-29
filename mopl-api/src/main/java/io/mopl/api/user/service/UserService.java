package io.mopl.api.user.service;

import io.mopl.api.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  //  /** 회원가입 */
  //  @Transactional
  //  public UserDto createUser(UserCreateRequest request) {
  //    }
  //  }

}
