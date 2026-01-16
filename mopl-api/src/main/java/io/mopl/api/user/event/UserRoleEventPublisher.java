package io.mopl.api.user.event;

import io.mopl.core.event.user.UserRoleChangedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRoleEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publish(UserRoleChangedEvent event) {
    kafkaTemplate.send(KafkaTopics.USER_ROLE_CHANGED, event.userId(), event);
  }
}
