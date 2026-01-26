package io.mopl.worker.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Kafka 소비 처리용 비동기 실행기 설정. */
@Configuration
@EnableAsync
public class AsyncConfig {

  /**
   * Kafka 처리 전용 TaskExecutor를 등록한다.
   *
   * @param properties 실행기 설정 프로퍼티
   * @return Executor 인스턴스
   */
  @Bean(name = "kafkaTaskExecutor")
  public Executor kafkaTaskExecutor(
      AsyncExecutorProperties properties, MeterRegistry meterRegistry) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.corePoolSize());
    executor.setMaxPoolSize(properties.maxPoolSize());
    executor.setQueueCapacity(properties.queueCapacity());
    executor.setThreadNamePrefix(properties.threadNamePrefix());
    executor.setRejectedExecutionHandler(
        new MeteredCallerRunsPolicy(meterRegistry, "kafkaTaskExecutor"));
    executor.initialize();
    return executor;
  }
}
