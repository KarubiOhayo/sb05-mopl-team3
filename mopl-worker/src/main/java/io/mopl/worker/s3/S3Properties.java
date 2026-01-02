package io.mopl.worker.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(String bucket, String region, String accessKey, String secretKey) {}
