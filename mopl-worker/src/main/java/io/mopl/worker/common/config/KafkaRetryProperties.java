package io.mopl.worker.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Kafka 재시도 정책 설정 프로퍼티. */
@ConfigurationProperties(prefix = "mopl.kafka.retry")
public record KafkaRetryProperties(
    Integer maxAttempts, Long initialBackoffMs, Double backoffMultiplier, Long maxBackoffMs) {}
