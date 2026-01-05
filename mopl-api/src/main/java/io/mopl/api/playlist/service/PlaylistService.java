package io.mopl.api.playlist.service;

import io.mopl.api.common.error.ContentErrorCode;
import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.playlist.domain.Playlist;
import io.mopl.api.playlist.domain.PlaylistContent;
import io.mopl.api.playlist.domain.PlaylistContentId;
import io.mopl.api.playlist.domain.PlaylistSubscription;
import io.mopl.api.playlist.domain.PlaylistSubscriptionId;
import io.mopl.api.playlist.dto.PlaylistCreateRequest;
import io.mopl.api.playlist.dto.PlaylistDto;
import io.mopl.api.playlist.repository.PlaylistContentRepository;
import io.mopl.api.playlist.repository.PlaylistRepository;
import io.mopl.api.playlist.repository.PlaylistSubscriptionRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserService userService;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final ContentRepository contentRepository;
  private final PlaylistContentRepository playlistContentRepository;

  // Playlist 생성
  @Transactional
  public PlaylistDto create(PlaylistCreateRequest request, UUID userId) {
    if (userId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    // 사용자 존재 여부를 먼저 확인
    UserSummary owner = userService.getUserSummary(userId);

    Playlist playlist =
        Playlist.builder()
            .ownerId(userId)
            .title(request.getTitle())
            .description(request.getDescription())
            .build();

    Playlist saved = playlistRepository.save(playlist);

    return PlaylistDto.builder()
        .id(saved.getId())
        .owner(owner)
        .title(saved.getTitle())
        .description(saved.getDescription())
        .updatedAt(saved.getUpdatedAt())
        .subscriberCount(saved.getSubscriberCount())
        .subscribedByMe(false)
        .contents(List.of())
        .build();
  }

  // Playlist 구독
  @Transactional
  public void subscribe(UUID playlistId, UUID userId) {
    validateSubscriptionInputs(playlistId, userId);

    assertPlaylistExists(playlistId);

    PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, userId);
    if (playlistSubscriptionRepository.existsById(id)) {
      return;
    }

    PlaylistSubscription subscription = PlaylistSubscription.builder().id(id).build();
    playlistSubscriptionRepository.save(subscription);
    int affected = playlistRepository.increaseSubscriberCount(playlistId);
    if (affected == 0) {
      log.warn(
          "playlist_subscribe_count_mismatch playlistId={} userId={} reason=update_not_applied",
          playlistId,
          userId);
    }
  }

  // Playlist 구독 취소
  @Transactional
  public void unsubscribe(UUID playlistId, UUID userId) {
    validateSubscriptionInputs(playlistId, userId);

    assertPlaylistExists(playlistId);

    PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, userId);
    if (!playlistSubscriptionRepository.existsById(id)) {
      return;
    }

    playlistSubscriptionRepository.deleteById(id);
    int affected = playlistRepository.decreaseSubscriberCount(playlistId);
    if (affected == 0) {
      log.warn(
          "playlist_unsubscribe_count_mismatch playlistId={} userId={} reason=subscriberCount_already_zero",
          playlistId,
          userId);
    }
  }

  // Playlist 컨텐츠 추가
  @Transactional
  public void addContent(UUID playlistId, UUID contentId, UUID userId) {
    validatePlaylistContentInputs(playlistId, contentId, userId);

    Playlist playlist = findPlaylistOrThrow(playlistId);

    assertOwner(playlist, userId);

    if (!contentRepository.existsById(contentId)) {
      throw new BusinessException(ContentErrorCode.CONTENT_NOT_FOUND);
    }

    PlaylistContentId id = new PlaylistContentId(playlistId, contentId);
    if (playlistContentRepository.existsById(id)) {
      return;
    }
    try {
      PlaylistContent playlistContent = PlaylistContent.builder().id(id).build();
      playlistContentRepository.save(playlistContent);
      log.info("playlist_content_added playlistId={} contentId={}", playlistId, contentId);
    } catch (DataIntegrityViolationException e) {
      // 동시 요청으로 인한 중복 저장 시도 - 이미 존재하므로 무시
      log.debug(
          "playlist_content_already_exists playlistId={} contentId={}", playlistId, contentId);
    }
  }

  // Playlist 컨텐츠 삭제
  @Transactional
  public void removeContent(UUID playlistId, UUID contentId, UUID userId) {
    validatePlaylistContentInputs(playlistId, contentId, userId);

    Playlist playlist = findPlaylistOrThrow(playlistId);

    assertOwner(playlist, userId);

    PlaylistContentId id = new PlaylistContentId(playlistId, contentId);
    if (!playlistContentRepository.existsById(id)) {
      return;
    }

    playlistContentRepository.deleteById(id);
    log.info("playlist_content_removed playlistId={} contentId={}", playlistId, contentId);
  }

  // -- 헬퍼 메서드 --
  private void validateSubscriptionInputs(UUID playlistId, UUID userId) {
    if (userId == null || playlistId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
  }

  private void assertPlaylistExists(UUID playlistId) {
    if (!playlistRepository.existsById(playlistId)) {
      throw new BusinessException(CommonErrorCode.NOT_FOUND);
    }
  }

  private void validatePlaylistContentInputs(UUID playlistId, UUID contentId, UUID userId) {
    if (userId == null || playlistId == null || contentId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
  }

  private Playlist findPlaylistOrThrow(UUID playlistId) {
    return playlistRepository
        .findById(playlistId)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
  }

  private void assertOwner(Playlist playlist, UUID userId) {
    if (!playlist.getOwnerId().equals(userId)) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }
  }
}
