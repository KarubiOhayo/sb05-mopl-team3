package io.mopl.worker.s3;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

  private final S3Presigner s3Presigner;
  private final S3Properties s3Properties;

  public String generatePresignedUrl(String s3Key) {
    if (s3Key == null || s3Key.isBlank()) {
      return null;
    }

    // 이미 URL 형태라면 그대로 반환 (외부 이미지 등)
    if (s3Key.startsWith("http://") || s3Key.startsWith("https://")) {
      return s3Key;
    }

    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(s3Properties.bucket()).key(s3Key).build();

      GetObjectPresignRequest presignRequest =
          GetObjectPresignRequest.builder()
              .signatureDuration(Duration.ofMinutes(60)) // 1시간 유효
              .getObjectRequest(getObjectRequest)
              .build();

      return s3Presigner.presignGetObject(presignRequest).url().toString();
    } catch (Exception e) {
      log.error("Presigned URL 생성 실패: s3Key={}", s3Key, e);
      return null;
    }
  }
}
