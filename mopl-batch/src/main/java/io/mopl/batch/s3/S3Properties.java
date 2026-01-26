package io.mopl.batch.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** S3 연결에 필요한 설정 프로퍼티. */
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(String bucket, String region, String accessKey, String secretKey) {}
