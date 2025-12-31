package io.mopl.worker.thumbnail;

import io.mopl.worker.s3.S3Properties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailS3Uploader {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

  private final S3Client s3Client;
  private final S3Properties s3Properties;
  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .connectTimeout(CONNECT_TIMEOUT)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  public void uploadFromUrl(String sourceUrl, String s3Key) throws Exception {
    String bucket = requireBucket();

    if (objectExists(bucket, s3Key)) {
      log.info("Skip upload: object already exists. s3Key={}", s3Key);
      return;
    }

    HttpRequest request =
        HttpRequest.newBuilder(URI.create(sourceUrl)).timeout(REQUEST_TIMEOUT).GET().build();

    HttpResponse<byte[]> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

    if (response.statusCode() / 100 != 2) {
      throw new IllegalStateException("HTTP download failed. status=" + response.statusCode());
    }

    byte[] body = response.body();
    if (body == null || body.length == 0) {
      throw new IllegalStateException("HTTP download returned empty body.");
    }

    String contentType = resolveContentType(response, s3Key);
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucket).key(s3Key).contentType(contentType).build();
    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(body));
  }

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

  private String resolveContentType(HttpResponse<byte[]> response, String s3Key) {
    String header =
        response.headers().firstValue("Content-Type").orElseGet(() -> guessFromKey(s3Key));
    if (header == null || header.isBlank()) {
      return "application/octet-stream";
    }
    int separator = header.indexOf(';');
    return (separator >= 0 ? header.substring(0, separator) : header).trim();
  }

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

  private String requireBucket() {
    if (s3Properties.bucket() == null || s3Properties.bucket().isBlank()) {
      throw new IllegalStateException("S3 bucket is not configured.");
    }
    return s3Properties.bucket();
  }
}
