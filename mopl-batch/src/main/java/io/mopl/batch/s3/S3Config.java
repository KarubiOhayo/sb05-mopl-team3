package io.mopl.batch.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/** S3 클라이언트 설정. */
@Configuration
public class S3Config {

  /**
   * 자격 증명 설정에 따라 S3Client를 생성한다.
   *
   * @param properties S3 설정 프로퍼티
   * @return S3Client 인스턴스
   */
  @Bean
  public S3Client s3Client(S3Properties properties) {
    S3ClientBuilder builder = S3Client.builder().region(Region.of(properties.region()));

    if (properties.accessKey() != null
        && !properties.accessKey().isBlank()
        && properties.secretKey() != null
        && !properties.secretKey().isBlank()) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
    }

    return builder.build();
  }
}
