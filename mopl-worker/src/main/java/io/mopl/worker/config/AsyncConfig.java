package io.mopl.worker.config;

import java.util.Optional;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "kafkaTaskExecutor")
  public Executor kafkaTaskExecutor(AsyncExecutorProperties properties) {
    int processors = Runtime.getRuntime().availableProcessors();
    int defaultCorePoolSize = Math.max(2, processors);
    int corePoolSize = Optional.ofNullable(properties.corePoolSize()).orElse(defaultCorePoolSize);
    int defaultMaxPoolSize = Math.max(corePoolSize, defaultCorePoolSize * 2);
    int maxPoolSize = Optional.ofNullable(properties.maxPoolSize()).orElse(defaultMaxPoolSize);
    int queueCapacity = Optional.ofNullable(properties.queueCapacity()).orElse(200);
    String threadNamePrefix =
        Optional.ofNullable(properties.threadNamePrefix()).orElse("kafka-async-");

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix(threadNamePrefix);
    executor.initialize();
    return executor;
  }
}
