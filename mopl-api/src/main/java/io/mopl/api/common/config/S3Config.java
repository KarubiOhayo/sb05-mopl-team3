package io.mopl.api.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
@RequiredArgsConstructor
public class S3Config {

  private final S3Properties s3properties;

  @Bean
  public S3Client s3Client() {
    S3ClientBuilder builder = S3Client.builder().region(Region.of(s3properties.getRegion()));

    if (hasText(s3properties.getAccessKey()) && hasText(s3properties.getSecretKey())) {
      AwsBasicCredentials credentials =
          AwsBasicCredentials.create(s3properties.getAccessKey(), s3properties.getSecretKey());
      builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
    }

    return builder.build();
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
