package io.mopl.api.content.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.mopl.api.common.config.S3Properties;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentThumbnailUploadService {

	private final S3Client s3Client;
	private final S3Properties s3Properties;

	private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

	public String uploadThumbnail(MultipartFile file) {
		log.info("썸네일 업로드 시작 - name: {}, size: {}, empty: {}, type: {}",
			file.getOriginalFilename(), file.getSize(), file.isEmpty(), file.getContentType());

		validateImage(file);

		String fileName = generateThumbnailFileName(Objects.requireNonNull(file.getOriginalFilename()));
		String key = s3Properties.getContentThumbnailPath() + fileName;
		log.debug("S3 key 생성 - key: {}", key);

		String contentType = file.getContentType();
		if (contentType == null || contentType.isBlank()) {
			contentType = DEFAULT_CONTENT_TYPE;
		}

		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(s3Properties.getBucket())
				.key(key)
				.contentType(contentType)
				.serverSideEncryption(ServerSideEncryption.AES256)
				.build();

			s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
			log.info("썸네일 업로드 성공 - key: {}", key);

			return generatePublicUrl(key);
		} catch (IOException | SdkException e) {
			log.error("컨텐츠 썸네일 업로드 실패", e);
		    throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

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

	private boolean isAllowedExtension(String originalFileName) {
		int dotIndex = originalFileName.lastIndexOf('.');
		if (dotIndex == -1 || dotIndex == originalFileName.length() - 1) {
			return false;
		}
		String extension = originalFileName.substring(dotIndex + 1).toLowerCase();
		return ALLOWED_EXTENSIONS.contains(extension);
	}

	private String generateThumbnailFileName(String originalFilename) {
		String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
		return "content_" + UUID.randomUUID() + "."	+ extension;
	}

	private String generatePublicUrl(String key) {
		return String.format(
			"https://%s.s3.%s.amazonaws.com/%s",
			s3Properties.getBucket(), s3Properties.getRegion(), key);
	}
}
