package io.mopl.api.user.service;

import io.mopl.api.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLinkedProviderService {

  private final UserRepository userRepository;
}
