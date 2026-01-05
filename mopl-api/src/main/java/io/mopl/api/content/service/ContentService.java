package io.mopl.api.content.service;

import io.mopl.api.common.error.ContentErrorCode;
import io.mopl.api.content.domain.Content;
import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.content.domain.ContentTagRepository;
import io.mopl.api.content.dto.ContentDto;
import io.mopl.core.error.BusinessException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;

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
}
