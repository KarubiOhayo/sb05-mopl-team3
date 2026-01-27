package io.mopl.worker.common.config;

import io.mopl.core.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
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
    // 1. 실패 시 DLT(Dead Letter Topic)로 발행하는 Recoverer
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (r, e) -> {
              log.error("Kafka 메시지 처리 최종 실패. DLQ로 이동: topic={}, key={}", r.topic(), r.key(), e);
              return new TopicPartition(r.topic() + ".dlq", -1);
            });

    // 2. 재시도 정책: 1초 간격, 최대 3회 시도
    FixedBackOff backOff = new FixedBackOff(1000L, 3);
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

    // 3. 재시도 하지 않을 예외 등록
    errorHandler.addNotRetryableExceptions(BusinessException.class);
    errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

    return errorHandler;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object>
      manualAckKafkaListenerContainerFactory(
          ConsumerFactory<String, Object> consumerFactory, CommonErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
    factory.getContainerProperties().setAsyncAcks(true);
    return factory;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object>
      notificationBatchKafkaListenerContainerFactory(
          ConsumerFactory<String, Object> consumerFactory, CommonErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);
    factory.setBatchListener(true);
    factory.getContainerProperties().setPollTimeout(200L);
    return factory;
  }
}
