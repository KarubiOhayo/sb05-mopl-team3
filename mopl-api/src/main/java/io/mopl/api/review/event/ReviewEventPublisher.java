package io.mopl.api.review.event;

import io.mopl.core.event.review.ReviewCreatedEvent;
import io.mopl.core.event.review.ReviewDeletedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publish(ReviewCreatedEvent event) {
    kafkaTemplate.send(KafkaTopics.REVIEW_CREATED, event.contentId(), event);
  }

  public void publish(ReviewDeletedEvent event) {
    kafkaTemplate.send(KafkaTopics.REVIEW_DELETED, event.contentId(), event);
  }
}
