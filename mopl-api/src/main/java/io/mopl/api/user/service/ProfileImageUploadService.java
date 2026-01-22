package io.mopl.api.user.service;

import io.mopl.api.common.config.S3Properties;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageUploadService {

  private final S3Client s3Client;
  private final S3Properties s3Properties;
  private final S3Presigner s3Presigner;

  private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");
  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

  /** 프로필 이미지 업로드 */
  public String uploadProfileImage(MultipartFile file, UUID userId) {
    validateImage(file);

    String fileName =
        generateProfileImageFileName(userId, Objects.requireNonNull(file.getOriginalFilename()));
    String key = s3Properties.getProfileImagePath() + fileName;

    String contentType = file.getContentType();
    if (contentType == null || contentType.isBlank()) {
      contentType = DEFAULT_CONTENT_TYPE;
    }

    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(s3Properties.getBucket())
              .key(key)
              .contentType(contentType)
              .serverSideEncryption(ServerSideEncryption.AES256)
              .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

      return key;

    } catch (IOException | SdkException e) {
      log.error("프로필 이미지 업로드 실패 - userId: {}", userId, e);
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  /** S3 키로 Presigned URL 생성 */
  public String generatePresignedUrl(String key) {
    if (key == null || key.isBlank()) {
      return null;
    }

    if (key.startsWith("https://") || key.startsWith("http://")) {
      return key;
    }

    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(s3Properties.getBucket()).key(key).build();

      GetObjectPresignRequest presignRequest =
          GetObjectPresignRequest.builder()
              .signatureDuration(
                  Duration.ofSeconds(s3Properties.getPresignedUrlExpirationSeconds()))
              .getObjectRequest(getObjectRequest)
              .build();

      PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
      return presigned.url().toString();
    } catch (SdkException e) {
      return null;
    }
  }

  /** 프로필 이미지 삭제 */
  public void deleteImage(String key) {
    if (key == null || key.isBlank()) {
      return;
    }

    if (key.startsWith("https://") || key.startsWith("http://")) {
      return;
    }

    try {
      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(s3Properties.getBucket()).key(key).build();

      s3Client.deleteObject(deleteObjectRequest);

    } catch (SdkException e) {
      log.error("이미지 삭제 실패 - key: {}", key, e);
    }
  }

  /** 이미지 유효성 검사 */
  private void validateImage(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || !isAllowedExtension(originalFilename)) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
  }

  /** 허용된 확장자인지 확인 */
  private boolean isAllowedExtension(String originalFilename) {
    if (originalFilename == null || originalFilename.isBlank()) {
      return false;
    }

    int dotIndex = originalFilename.lastIndexOf('.');

    if (dotIndex == -1 || dotIndex == originalFilename.length() - 1) {
      return false;
    }

    String extension = originalFilename.substring(dotIndex + 1).toLowerCase();
    return ALLOWED_EXTENSIONS.contains(extension);
  }

  /** 프로필 이미지 파일명 생성 */
  private String generateProfileImageFileName(UUID userId, String originalFilename) {
    String extension =
        originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    long timestamp = System.currentTimeMillis();
    return userId + "_" + timestamp + "." + extension;
  }
}
