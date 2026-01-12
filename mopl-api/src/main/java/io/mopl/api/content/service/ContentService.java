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
import io.mopl.api.content.dto.ContentUpdateRequest;
import io.mopl.api.content.dto.CursorResponseContentDto;
import io.mopl.api.content.event.ThumbnailDeleteAfterCommitEvent;
import io.mopl.api.content.event.ThumbnailUploadedEvent;
import io.mopl.api.playlist.repository.PlaylistContentRepository;
import io.mopl.api.review.repository.ReviewRepository;
import io.mopl.core.error.BusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public ContentDto create(ContentCreateRequest contentCreateRequest, MultipartFile thumbnail) {
    log.info(
        "컨텐츠 생성 시작 - type: {}, titleLen: {}, tags: {}, thumbnail: {}",
        contentCreateRequest.getType(),
        contentCreateRequest.getTitle() != null ? contentCreateRequest.getTitle().length() : 0,
        contentCreateRequest.getTags() != null ? contentCreateRequest.getTags().size() : 0,
        (thumbnail != null && !thumbnail.isEmpty()));

    String title = contentCreateRequest.getTitle();
    String description = contentCreateRequest.getDescription();

    String thumbnailUrl = null;
    if (thumbnail != null && !thumbnail.isEmpty()) {
      thumbnailUrl = contentThumbnailUploadService.uploadThumbnail(thumbnail, contentCreateRequest.getType());
      log.info("썸네일 업로드 완료 - urlLen: {}", thumbnailUrl != null ? thumbnailUrl.length() : 0);
    }

    Content content =
        Content.builder()
            .type(contentCreateRequest.getType())
            .title(title)
            .description(description)
            .thumbnailUrl(thumbnailUrl)
            .build();
    contentRepository.save(content);

    log.info("컨텐츠 저장 완료 - contentId: {}", content.getId());

    List<String> tagNames = contentCreateRequest.getTags();

    if (tagNames != null && !tagNames.isEmpty()) {
      for (String tagName : tagNames) {
        Tag newTag =
            tagRepository.findByName(tagName).orElseGet(() -> tagRepository.save(new Tag(tagName)));

        ContentTag contentTag =
            ContentTag.builder().id(new ContentTagId(content.getId(), newTag.getId())).build();

        contentTagRepository.save(contentTag);
      }
    }

    log.info("컨텐츠 생성을 완료했습니다.");
    return new ContentDto(
        content.getId(),
        content.getType(),
        content.getTitle(),
        content.getDescription(),
        content.getThumbnailUrl(),
        tagNames,
        0.0,
        0,
        0L);
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
    String thumbnailUrl =
        contentThumbnailUploadService.generatePresignedUrl(content.getThumbnailUrl());
    return new ContentDto(
        content.getId(),
        content.getType(),
        content.getTitle(),
        content.getDescription(),
        thumbnailUrl,
        tagNames,
        content.getAverageRating(),
        content.getReviewCount(),
        content.getWatcherCount());
  }

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(UUID contentId) {
    log.info("컨텐츠 삭제 시작: contentId: {}", contentId);
    Content content =
        contentRepository
            .findById(contentId)
            .orElseThrow(() -> new BusinessException(ContentErrorCode.CONTENT_NOT_FOUND));
    reviewRepository.deleteByContentId(contentId);
    playlistContentRepository.deleteByIdContentId(contentId);
    contentTagRepository.deleteByIdContentId(contentId);
    contentThumbnailUploadService.deleteThumbnail(content.getThumbnailUrl());
    contentRepository.deleteById(contentId);
    log.info("컨텐츠 삭제 완료: contentId: {}", contentId);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public ContentDto update(
      UUID contentId, ContentUpdateRequest contentUpdateRequest, MultipartFile thumbnail) {
    log.debug("컨텐츠 수정 시작: contentId={}, request = {}", contentId, contentUpdateRequest);

    Content content =
        contentRepository
            .findById(contentId)
            .orElseThrow(() -> new BusinessException(ContentErrorCode.CONTENT_NOT_FOUND));

    String title = contentUpdateRequest.getTitle();
    String description = contentUpdateRequest.getDescription();

    String deletedUrl = content.getThumbnailUrl();
    String updatedUrl = deletedUrl;

    boolean hasNewThumbnail = thumbnail != null && !thumbnail.isEmpty();
    if (hasNewThumbnail) {
      updatedUrl = contentThumbnailUploadService.uploadThumbnail(thumbnail, content.getType());
      eventPublisher.publishEvent(new ThumbnailUploadedEvent(updatedUrl));
    }
    content.update(title, description, updatedUrl);

    List<String> requestedTags = contentUpdateRequest.getTags();
    if (requestedTags != null) {
      Set<String> requested = new HashSet<>(requestedTags);
      Set<String> existing = new HashSet<>(contentTagRepository.findTagNamesByContentId(contentId));

      Set<String> toRemove = new HashSet<>(existing);
      toRemove.removeAll(requested);

      Set<String> toAdd = new HashSet<>(requested);
      toAdd.removeAll(existing);

      if (!toRemove.isEmpty()) {
        List<Tag> removeTags = tagRepository.findByNameIn(toRemove);
        List<UUID> removeTagIds = removeTags.stream().map(Tag::getId).toList();
        if (!removeTagIds.isEmpty()) {
          contentTagRepository.deleteByContentIdAndTagIdIn(contentId, removeTagIds);
        }
      }

      if (!toAdd.isEmpty()) {
        List<Tag> existingTags = tagRepository.findByNameIn(toAdd);
        Map<String, Tag> tagByName =
            existingTags.stream().collect(Collectors.toMap(Tag::getName, t -> t, (a, b) -> a));

        for (String tagName : toAdd) {
          Tag tag = tagByName.get(tagName);
          if (tag == null) {
            tag = tagRepository.save(new Tag(tagName));
          }
          contentTagRepository.save(
              ContentTag.builder().id(new ContentTagId(contentId, tag.getId())).build());
        }
      }
    } else {
      requestedTags = contentTagRepository.findTagNamesByContentId(contentId);
    }

    if (hasNewThumbnail && deletedUrl != null) {
      eventPublisher.publishEvent(new ThumbnailDeleteAfterCommitEvent(deletedUrl));
    }

    log.info("컨텐츠 수정을 완료하였습니다. contentId: {}", contentId);
    String thumbnailUrl =
        contentThumbnailUploadService.generatePresignedUrl(content.getThumbnailUrl());
    return new ContentDto(
        content.getId(),
        content.getType(),
        content.getTitle(),
        content.getDescription(),
        thumbnailUrl,
        requestedTags,
        content.getAverageRating(),
        content.getReviewCount(),
        content.getWatcherCount());
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
                        contentThumbnailUploadService.generatePresignedUrl(c.getThumbnailUrl()),
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
