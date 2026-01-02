package io.mopl.worker.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class S3Config {

  @Bean
  public S3Client s3Client(S3Properties properties) {
    S3ClientBuilder builder = S3Client.builder().region(Region.of(properties.region()));

    if (hasText(properties.accessKey()) && hasText(properties.secretKey())) {
      AwsBasicCredentials credentials =
          AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());
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
