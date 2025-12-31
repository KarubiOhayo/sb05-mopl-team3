package io.mopl.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mopl.kafka.retry")
public record KafkaRetryProperties(
    Integer maxAttempts, Long initialBackoffMs, Double backoffMultiplier, Long maxBackoffMs) {}
