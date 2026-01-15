package io.mopl.socket.common.config;

import io.mopl.core.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  @Bean
  public CommonErrorHandler errorHandler() {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (r, e) -> {
              log.error("Kafka 메시지 처리 최종 실패. DLQ로 이동: topic={}, key={}", r.topic(), r.key(), e);
              return new TopicPartition(r.topic() + ".DLQ", r.partition());
            });

    FixedBackOff backOff = new FixedBackOff(1000L, 3);
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    errorHandler.addNotRetryableExceptions(BusinessException.class);
    errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

    return errorHandler;
  }
}
