package io.mopl.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.async")
public record AsyncExecutorProperties(
    Integer corePoolSize, Integer maxPoolSize, Integer queueCapacity, String threadNamePrefix) {}
