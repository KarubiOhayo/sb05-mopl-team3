package io.mopl.api.follow.event;

import io.mopl.core.event.follow.UserFollowedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowEventPublisher {
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publish(UserFollowedEvent event) {
    kafkaTemplate.send(KafkaTopics.USER_FOLLOWED, event.followeeId(), event);
  }
}
