package io.mopl.api.content.service;

import io.mopl.api.common.error.ContentErrorCode;
import io.mopl.api.content.domain.Content;
import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.content.domain.ContentTag;
import io.mopl.api.content.domain.ContentTagId;
import io.mopl.api.content.domain.ContentTagRepository;
import io.mopl.api.content.domain.Tag;
import io.mopl.api.content.domain.TagRepository;
import io.mopl.api.content.dto.ContentCreateRequest;
import io.mopl.api.content.dto.ContentDto;
import io.mopl.api.content.dto.ContentPage;
import io.mopl.api.content.dto.ContentSearchRequest;
import io.mopl.api.content.dto.CursorResponseContentDto;
import io.mopl.api.playlist.repository.PlaylistContentRepository;
import io.mopl.api.review.repository.ReviewRepository;
import io.mopl.core.error.BusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;
  private final ReviewRepository reviewRepository;
  private final PlaylistContentRepository playlistContentRepository;
  private final TagRepository tagRepository;
  private final ContentThumbnailUploadService contentThumbnailUploadService;

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ContentDto create(ContentCreateRequest contentCreateRequest, MultipartFile thumbnail) {
		log.info("컨텐츠 생성 시작 - type: {}, titleLen: {}, tags: {}, thumbnail: {}",
			contentCreateRequest.getType(),
			contentCreateRequest.getTitle() != null ? contentCreateRequest.getTitle().length() : 0,
			contentCreateRequest.getTags() != null ? contentCreateRequest.getTags().size() : 0,
			(thumbnail != null && !thumbnail.isEmpty()));

		String title = contentCreateRequest.getTitle();
		String description = contentCreateRequest.getDescription();

		String thumbnailUrl = null;
		if (thumbnail != null && !thumbnail.isEmpty()) {
			thumbnailUrl = contentThumbnailUploadService.uploadThumbnail(thumbnail);
			log.info("썸네일 업로드 완료 - urlLen: {}", thumbnailUrl != null ? thumbnailUrl.length() : 0);
		}

		Content content = Content.builder()
			.type(contentCreateRequest.getType())
			.title(title)
			.description(description)
			.thumbnailUrl(thumbnailUrl)
			.build();
		contentRepository.save(content);

		log.info("컨텐츠 저장 완료 - contentId: {}", content.getId());

		List<String> tagNames = contentCreateRequest.getTags();

		for (String tagName : tagNames) {
			Tag newTag = tagRepository.findByName(tagName)
				.orElseGet(()-> tagRepository.save(new Tag(tagName)));

			ContentTag contentTag = ContentTag.builder()
				.id(new ContentTagId(content.getId(), newTag.getId()))
				.build();

			contentTagRepository.save(contentTag);
		}

		log.info("컨텐츠 생성을 완료했습니다.");
		return new ContentDto(content.getId(), content.getType(), content.getTitle(),
			content.getDescription(), content.getThumbnailUrl(), tagNames,
			0.0, 0, 0L);
	}

  @Transactional(readOnly = true)
  public ContentDto findById(UUID contentId) {
    log.info("컨텐츠 단건 조회를 시작합니다. contentId: {}", contentId);

    Content content =
        contentRepository
            .findById(contentId)
            .orElseThrow(() -> new BusinessException(ContentErrorCode.CONTENT_NOT_FOUND));

    List<String> tagNames = contentTagRepository.findTagNamesByContentId(contentId);

    log.info("컨텐츠 조회를 완료했습니다. contentId: {}", contentId);
    return new ContentDto(
        content.getId(),
        content.getType(),
        content.getTitle(),
        content.getDescription(),
        content.getThumbnailUrl(),
        tagNames,
        content.getAverageRating(),
        content.getReviewCount(),
        content.getWatcherCount());
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(UUID contentId) {
    log.info("컨텐츠 삭제 시작: contentId: {}", contentId);
    contentRepository
        .findById(contentId)
        .orElseThrow(() -> new BusinessException(ContentErrorCode.CONTENT_NOT_FOUND));
    reviewRepository.deleteByContentId(contentId);
    playlistContentRepository.deleteByIdContentId(contentId);
    contentTagRepository.deleteByIdContentId(contentId);
    contentRepository.deleteById(contentId);
    log.info("컨텐츠 삭제 완료: contentId: {}", contentId);
  }

  @Transactional(readOnly = true)
  public CursorResponseContentDto findAll(ContentSearchRequest contentSearchRequest) {

    String sortBy = contentSearchRequest.getSortByOrDefault();
    String sortDirection = contentSearchRequest.getSortDirectionOrDefault();

    ContentPage page = contentRepository.findContentPage(contentSearchRequest);

    long totalCount =
        contentRepository.countContents(
            contentSearchRequest.getTypeEqual(),
            contentSearchRequest.getKeywordLike(),
            contentSearchRequest.getTagsIn());

    List<Content> contents = page.getContents();
    List<UUID> contentIds = contents.stream().map(Content::getId).toList();

    Map<UUID, List<String>> tagsByContentId = new HashMap<>();
    for (Object[] row : contentTagRepository.findTagNamesByContentIds(contentIds)) {
      UUID contentId = (UUID) row[0];
      String tagName = (String) row[1];
      tagsByContentId.computeIfAbsent(contentId, k -> new ArrayList<>()).add(tagName);
    }

    List<ContentDto> data =
        contents.stream()
            .map(
                c ->
                    new ContentDto(
                        c.getId(),
                        c.getType(),
                        c.getTitle(),
                        c.getDescription(),
                        c.getThumbnailUrl(),
                        tagsByContentId.getOrDefault(c.getId(), List.of()),
                        c.getAverageRating(),
                        c.getReviewCount(),
                        c.getWatcherCount()))
            .toList();

    return CursorResponseContentDto.builder()
        .data(data)
        .nextCursor(page.getNextCursor())
        .nextIdAfter(page.getNextIdAfter())
        .hasNext(page.isHasNext())
        .totalCount(totalCount)
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .build();
  }
}
