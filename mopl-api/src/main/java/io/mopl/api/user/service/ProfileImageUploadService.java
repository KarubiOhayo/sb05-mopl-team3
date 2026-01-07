package io.mopl.api.user.service;

import io.mopl.api.common.config.S3Properties;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageUploadService {

  private final S3Client s3Client;
  private final S3Properties s3Properties;

  private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");
  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

  /** 프로필 이미지 업로드 */
  public String uploadProfileImage(MultipartFile file, UUID userId) {
    validateImage(file);

    String fileName = generateProfileImageFileName(userId);
    String key = s3Properties.getProfileImagePath() + fileName;

    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(s3Properties.getBucket())
              .key(key)
              .contentType(file.getContentType())
              .serverSideEncryption(ServerSideEncryption.AES256)
              .build();

      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

      return generatePublicUrl(key);

    } catch (IOException | SdkException e) {
      log.error("프로필 이미지 업로드 실패 - userId: {}", userId, e);
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
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
    String extension =
        originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    return ALLOWED_EXTENSIONS.contains(extension);
  }

  /** 프로필 이미지 파일명 생성 */
  private String generateProfileImageFileName(UUID userId) {
    long timestamp = System.currentTimeMillis();
    return userId + "_" + timestamp + ".jpg";
  }

  /** Public URL 생성 */
  private String generatePublicUrl(String key) {
    return String.format(
        "https://%s.s3.%s.amazonaws.com/%s",
        s3Properties.getBucket(), s3Properties.getRegion(), key);
  }

  /** URL에서 S3 키 추출 */
  private String extractKeyFromUrl(String url) {
    // URL 형식: https://{bucket}.s3.{region}.amazonaws.com/{key}
    try {
      String[] parts = url.split("\\?")[0].split(s3Properties.getBucket() + "/");
      if (parts.length > 1) {
        return parts[1];
      }
      return url.split("amazonaws.com/")[1].split("\\?")[0];
    } catch (Exception e) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  /** 이미지 삭제 */
  public void deleteImageByUrl(String imageUrl) {
    if (imageUrl == null || imageUrl.isEmpty()) {
      return;
    }
    try {
      String key = extractKeyFromUrl(imageUrl);

      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(s3Properties.getBucket()).key(key).build();

      s3Client.deleteObject(deleteObjectRequest);

    } catch (Exception e) {
      log.warn("이미지 삭제 실패 - url: {}", imageUrl, e);
    }
  }
}
