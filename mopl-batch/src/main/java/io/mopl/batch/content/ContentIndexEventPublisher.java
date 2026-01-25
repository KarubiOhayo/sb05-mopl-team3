package io.mopl.batch.content;

import io.mopl.core.event.content.ContentIndexBatchRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentIndexEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publish(ContentIndexBatchRequestedEvent event) {
    kafkaTemplate.send(KafkaTopics.CONTENT_INDEX_REQUESTED, event.eventId(), event);
  }
}
