package io.mopl.batch.thumbnail;

import io.mopl.core.event.thumbnail.ThumbnailSourceType;

/**
 * 썸네일 생성 요청을 전달하기 위한 스프링 이벤트.
 *
 * @param contentId 콘텐츠 ID
 * @param sourceType 썸네일 원본 출처
 * @param sourceUrl 원본 이미지 URL
 * @param s3Key 저장될 S3 키
 * @param uploadMode 썸네일 업로드 방식
 */
public record ThumbnailRequestedSpringEvent(
    String contentId,
    ThumbnailSourceType sourceType,
    String sourceUrl,
    String s3Key,
    ThumbnailUploadMode uploadMode) {}
