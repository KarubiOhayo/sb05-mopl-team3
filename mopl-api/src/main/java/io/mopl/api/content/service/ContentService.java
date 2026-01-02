package io.mopl.api.content.service;

import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.content.domain.ContentTagRepository;
import io.mopl.api.playlist.repository.PlaylistContentRepository;
import io.mopl.api.review.repository.ReviewRepository;
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

  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(UUID contentId) {
    log.info("컨텐츠 삭제 시작: contentId: {}", contentId);
    reviewRepository.deleteByContentId(contentId);
    playlistContentRepository.deleteByIdContentId(contentId);
    contentTagRepository.deleteByIdContentId(contentId);
    contentRepository.deleteById(contentId);
    log.info("컨텐츠 삭제 완료: contentId: {}", contentId);
  }
}
