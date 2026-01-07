package io.mopl.api.content.service;

import io.mopl.api.common.error.ContentErrorCode;
import io.mopl.api.content.domain.Content;
import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.content.domain.ContentTagRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;
  private final ReviewRepository reviewRepository;
  private final PlaylistContentRepository playlistContentRepository;

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

  public CursorResponseContentDto findAll(ContentSearchRequest contentSearchRequest) {

    String sortBy = contentSearchRequest.getSortByOrDefault();
    String sortDirection = contentSearchRequest.getSortDirectionOrDefault();

    ContentPage page = contentRepository.findContentPage(contentSearchRequest);

    long totalCount = contentRepository.countContents(
        contentSearchRequest.getTypeEqual(),
        contentSearchRequest.getKeywordLike(),
        contentSearchRequest.getTagsIn()
    );

    List<Content> contents = page.getContents();
    List<UUID> contentIds = contents.stream().map(Content::getId).toList();

    Map<UUID, List<String>> tagsByContentId = new HashMap<>();
    for (Object[] row : contentTagRepository.findTagNamesByContentIds(contentIds)) {
      UUID contentId = (UUID) row[0];
      String tagName = (String) row[1];
      tagsByContentId.computeIfAbsent(contentId, k -> new ArrayList<>()).add(tagName);
    }

    List<ContentDto> data = contents.stream()
        .map(c -> new ContentDto(
            c.getId(),
            c.getType(),
            c.getTitle(),
            c.getDescription(),
            c.getThumbnailUrl(),
            tagsByContentId.getOrDefault(c.getId(), List.of()),
            c.getAverageRating(),
            c.getReviewCount(),
            c.getWatcherCount()
        ))
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
