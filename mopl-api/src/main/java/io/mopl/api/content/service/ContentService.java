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
import io.mopl.api.playlist.repository.PlaylistContentRepository;
import io.mopl.api.review.repository.ReviewRepository;
import io.mopl.core.error.BusinessException;
import java.util.List;
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
	public ContentDto create(ContentCreateRequest contentCreateRequest, MultipartFile thumbnail) {
		log.info("컨텐츠 생성을 시작합니다.");
		String title = contentCreateRequest.getTitle();
		String description = contentCreateRequest.getDescription();

		Content content = Content.builder()
			.type(contentCreateRequest.getType())
			.title(title)
			.description(description)
			// null 대신 dml에 있는 thumbnail_url 주소 아무거나 넣기
			.thumbnailUrl(null)
			.build();
		contentRepository.save(content);

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
}
