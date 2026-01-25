package io.mopl.batch.thumbnail;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.mopl.batch.common.BatchErrorCode;
import io.mopl.batch.s3.S3Properties;
import io.mopl.core.error.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * 배치 컨테이너에서 원본 썸네일 URL을 다운로드해 S3로 업로드한다.
 *
 * <p>이미 존재하는 객체는 업로드를 건너뛰며, 응답 상태/본문 검증을 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchThumbnailS3Uploader {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
  private static final ConcurrentHashMap<String, AtomicLong> LAST_COMPLETION_GAUGES =
      new ConcurrentHashMap<>();

  private final S3Client s3Client;
  private final S3Properties s3Properties;
  private final MeterRegistry meterRegistry;
  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .connectTimeout(CONNECT_TIMEOUT)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  /**
   * 원본 URL에서 이미지를 다운로드해 S3에 업로드한다.
   *
   * @param sourceUrl 원본 이미지 URL
   * @param s3Key 저장할 S3 키
   * @throws Exception 다운로드/업로드 실패 시
   */
  public void uploadFromUrl(String sourceUrl, String s3Key) throws Exception {
    Timer.Sample sample = Timer.start(meterRegistry);
    String runTag = RunIdResolver.resolveFromKey(s3Key);
    String bucket = requireBucket();

    try {
      if (objectExists(bucket, s3Key)) {
        log.info("업로드 건너뜀: 이미 객체가 존재합니다. s3Key={}", s3Key);
        Counter.builder("batch.thumbnail.upload.skipped")
            .tags("run_id", runTag)
            .register(meterRegistry)
            .increment();
        sample.stop(
            Timer.builder("batch.thumbnail.upload.duration")
                .tags("result", "skipped", "run_id", runTag)
                .register(meterRegistry));
        return;
      }

      HttpRequest request =
          HttpRequest.newBuilder(URI.create(sourceUrl)).timeout(REQUEST_TIMEOUT).GET().build();

      HttpResponse<byte[]> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      if (response.statusCode() / 100 != 2) {
        throw new BusinessException(BatchErrorCode.THUMBNAIL_DOWNLOAD_FAILED)
            .addDetail("status", String.valueOf(response.statusCode()));
      }

      byte[] body = response.body();
      if (body == null || body.length == 0) {
        throw new BusinessException(BatchErrorCode.THUMBNAIL_EMPTY_BODY);
      }

      String contentType = resolveContentType(response, s3Key);
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder().bucket(bucket).key(s3Key).contentType(contentType).build();
      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(body));
      sample.stop(
          Timer.builder("batch.thumbnail.upload.duration")
              .tags("result", "success", "run_id", runTag)
              .register(meterRegistry));
    } catch (Exception ex) {
      Counter.builder("batch.thumbnail.upload.errors")
          .tags("type", ex.getClass().getSimpleName(), "run_id", runTag)
          .register(meterRegistry)
          .increment();
      sample.stop(
          Timer.builder("batch.thumbnail.upload.duration")
              .tags("result", "failed", "run_id", runTag)
              .register(meterRegistry));
      throw ex;
    } finally {
      AtomicLong gauge =
          LAST_COMPLETION_GAUGES.computeIfAbsent(
              runTag,
              key -> {
                AtomicLong value = new AtomicLong();
                meterRegistry.gauge(
                    "batch.thumbnail.last_completion.epoch_ms",
                    io.micrometer.core.instrument.Tags.of("run_id", runTag),
                    value);
                return value;
              });
      gauge.set(System.currentTimeMillis());
    }
  }

  /** S3에 동일 키의 객체가 존재하는지 확인한다. */
  private boolean objectExists(String bucket, String s3Key) {
    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(s3Key).build());
      return true;
    } catch (NoSuchKeyException ex) {
      return false;
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return false;
      }
      throw ex;
    }
  }

  /** 응답 헤더 또는 확장자로 Content-Type을 추정한다. */
  private String resolveContentType(HttpResponse<byte[]> response, String s3Key) {
    String header =
        response.headers().firstValue("Content-Type").orElseGet(() -> guessFromKey(s3Key));
    if (header == null || header.isBlank()) {
      return "application/octet-stream";
    }
    int separator = header.indexOf(';');
    return (separator >= 0 ? header.substring(0, separator) : header).trim();
  }

  /** S3 키 확장자를 기준으로 Content-Type을 추정한다. */
  private static String guessFromKey(String s3Key) {
    String lower = s3Key.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    if (lower.endsWith(".webp")) {
      return "image/webp";
    }
    if (lower.endsWith(".gif")) {
      return "image/gif";
    }
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    return "application/octet-stream";
  }

  /** 버킷 설정이 있는지 확인하고 없으면 예외를 던진다. */
  private String requireBucket() {
    if (s3Properties.bucket() == null || s3Properties.bucket().isBlank()) {
      throw new BusinessException(BatchErrorCode.S3_BUCKET_NOT_CONFIGURED);
    }
    return s3Properties.bucket();
  }
}
