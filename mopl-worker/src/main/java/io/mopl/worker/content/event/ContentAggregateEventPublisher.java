package io.mopl.worker.content.event;

import io.mopl.core.event.content.ContentAggregateUpdatedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentAggregateEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publish(ContentAggregateUpdatedEvent event) {
    kafkaTemplate.send(KafkaTopics.CONTENT_AGGREGATE_UPDATED, event.contentId(), event);
  }
}
