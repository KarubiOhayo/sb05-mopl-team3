package io.mopl.worker.s3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
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
   * <p>accessKey/secretKey가 있으면 정적 자격 증명을, 없으면 기본 자격 증명 체인을 사용한다.
   *
   * @param properties S3 설정 프로퍼티
   * @return S3Client 인스턴스
   */
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

  /** 공백이 아닌 문자열인지 확인한다. */
  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
