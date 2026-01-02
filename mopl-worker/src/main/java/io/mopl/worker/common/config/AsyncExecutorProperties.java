package io.mopl.worker.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "mopl.async")
@Validated
public record AsyncExecutorProperties(
    @Min(1) Integer corePoolSize,
    @Min(1) Integer maxPoolSize,
    @Min(0) Integer queueCapacity,
    @NotBlank String threadNamePrefix) {

  private static final int DEFAULT_QUEUE_CAPACITY = 200;
  private static final String DEFAULT_THREAD_NAME_PREFIX = "kafka-async-";

  public AsyncExecutorProperties {
    int processors = Runtime.getRuntime().availableProcessors();
    int defaultCorePoolSize = Math.max(2, processors);
    int resolvedCorePoolSize = corePoolSize == null ? defaultCorePoolSize : corePoolSize;
    int defaultMaxPoolSize = Math.max(resolvedCorePoolSize, defaultCorePoolSize * 2);
    int resolvedMaxPoolSize = maxPoolSize == null ? defaultMaxPoolSize : maxPoolSize;
    int resolvedQueueCapacity = queueCapacity == null ? DEFAULT_QUEUE_CAPACITY : queueCapacity;
    String resolvedThreadNamePrefix =
        threadNamePrefix == null ? DEFAULT_THREAD_NAME_PREFIX : threadNamePrefix;

    corePoolSize = resolvedCorePoolSize;
    maxPoolSize = resolvedMaxPoolSize;
    queueCapacity = resolvedQueueCapacity;
    threadNamePrefix = resolvedThreadNamePrefix;
  }
}
