package io.mopl.api.playlist.service;

import io.mopl.api.common.error.ContentErrorCode;
import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.playlist.domain.Playlist;
import io.mopl.api.playlist.domain.PlaylistContentId;
import io.mopl.api.playlist.domain.PlaylistContentRepository;
import io.mopl.api.playlist.domain.PlaylistRepository;
import io.mopl.api.playlist.domain.PlaylistSubscription;
import io.mopl.api.playlist.domain.PlaylistSubscriptionId;
import io.mopl.api.playlist.domain.PlaylistSubscriptionRepository;
import io.mopl.api.playlist.dto.PlaylistCreateRequest;
import io.mopl.api.playlist.dto.PlaylistDto;
import io.mopl.api.playlist.dto.PlaylistUpdateRequest;
import io.mopl.api.playlist.event.PlaylistContentAddedInternalEvent;
import io.mopl.api.playlist.event.PlaylistCreatedInternalEvent;
import io.mopl.api.playlist.event.PlaylistSubscribedInternalEvent;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.redis.constants.RedisKeyPrefix;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserService userService;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final ContentRepository contentRepository;
  private final PlaylistContentRepository playlistContentRepository;
  private final PlaylistQueryService playlistQueryService;
  private final ApplicationEventPublisher eventPublisher;
  private final RedisTemplate<String, String> redisTemplate;

  @Transactional
  // 플레이리스트 생성 및 생성 이벤트 발행
  public PlaylistDto create(PlaylistCreateRequest request, UUID userId) {
    if (userId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    UserSummary owner = userService.getUserSummary(userId);

    Playlist playlist =
        Playlist.builder()
            .ownerId(userId)
            .title(request.getTitle())
            .description(request.getDescription())
            .build();

    Playlist saved = playlistRepository.save(playlist);

    eventPublisher.publishEvent(
        new PlaylistCreatedInternalEvent(saved.getId(), userId, owner.getName()));

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

  @Transactional
  // 플레이리스트 구독 처리 및 카운트 갱신
  public void subscribe(UUID playlistId, UUID userId) {
    validatePlaylistAndUserIds(playlistId, userId);

    Playlist playlist = findPlaylistOrThrow(playlistId);

    PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, userId);
    if (playlistSubscriptionRepository.existsById(id)) {
      return;
    }
    UserSummary subscriber = userService.getUserSummary(userId);

    PlaylistSubscription subscription = PlaylistSubscription.builder().id(id).build();
    playlistSubscriptionRepository.save(subscription);
    int affected = playlistRepository.increaseSubscriberCount(playlistId);
    if (affected == 0) {
      log.warn(
          "플레이리스트 구독 수 증가 실패 playlistId={} userId={} reason=update_not_applied",
          playlistId,
          userId);
    }
    runAfterCommit(
        () -> {
          try {
            String key = RedisKeyPrefix.PLAYLIST_SUBS_BY_USER + userId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
              redisTemplate.opsForSet().add(key, playlistId.toString());
            }
          } catch (Exception e) {
            log.warn(
                "레디스 캐시 갱신 실패 key={} error={}",
                RedisKeyPrefix.PLAYLIST_SUBS_BY_USER + userId,
                e.getMessage());
          }
        });

    eventPublisher.publishEvent(
        new PlaylistSubscribedInternalEvent(
            playlistId, playlist.getOwnerId(), userId, subscriber.getName()));
  }

  @Transactional
  // 플레이리스트 구독 해제 및 카운트 갱신
  public void unsubscribe(UUID playlistId, UUID userId) {
    validatePlaylistAndUserIds(playlistId, userId);

    assertPlaylistExists(playlistId);

    PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, userId);
    if (!playlistSubscriptionRepository.existsById(id)) {
      return;
    }

    playlistSubscriptionRepository.deleteById(id);
    int affected = playlistRepository.decreaseSubscriberCount(playlistId);
    if (affected == 0) {
      log.warn(
          "플레이리스트 구독 수 감소 실패 playlistId={} userId={} reason=subscriberCount_already_zero",
          playlistId,
          userId);
    }
    // 커밋 후 구독 캐시 갱신
    runAfterCommit(
        () -> {
          try {
            String key = RedisKeyPrefix.PLAYLIST_SUBS_BY_USER + userId;
            redisTemplate.opsForSet().remove(key, playlistId.toString());
          } catch (Exception e) {
            log.warn(
                "레디스 캐시 갱신 실패 key={} error={}",
                RedisKeyPrefix.PLAYLIST_SUBS_BY_USER + userId,
                e.getMessage());
          }
        });
  }

  @Transactional
  // 플레이리스트에 콘텐츠 추가 및 캐시 무효화
  public void addContent(UUID playlistId, UUID contentId, UUID userId) {
    validatePlaylistContentInputs(playlistId, contentId, userId);

    Playlist playlist = findPlaylistOrThrow(playlistId);

    assertOwner(playlist, userId);

    if (!contentRepository.existsById(contentId)) {
      throw new BusinessException(ContentErrorCode.CONTENT_NOT_FOUND);
    }

    int affected =
        playlistContentRepository.insertIgnore(playlistId.toString(), contentId.toString());
    if (affected == 0) {
      log.debug("플레이리스트에 이미 콘텐츠 존재 playlistId={} contentId={}", playlistId, contentId);
      return;
    }
    eventPublisher.publishEvent(
        new PlaylistContentAddedInternalEvent(playlistId, playlist.getOwnerId(), contentId));
    // 커밋 후 콘텐츠 캐시 삭제
    runAfterCommit(
        () -> {
          deletePlaylistContentCaches(playlistId);
        });
  }

  @Transactional
  // 플레이리스트에서 콘텐츠 삭제 및 캐시 무효화
  public void removeContent(UUID playlistId, UUID contentId, UUID userId) {
    validatePlaylistContentInputs(playlistId, contentId, userId);

    Playlist playlist = findPlaylistOrThrow(playlistId);

    assertOwner(playlist, userId);

    PlaylistContentId id = new PlaylistContentId(playlistId, contentId);
    if (!playlistContentRepository.existsById(id)) {
      return;
    }

    playlistContentRepository.deleteById(id);
    log.info("플레이리스트 콘텐츠 삭제 완료 playlistId={} contentId={}", playlistId, contentId);
    // 커밋 후 콘텐츠 캐시 삭제
    runAfterCommit(
        () -> {
          deletePlaylistContentCaches(playlistId);
        });
  }

  @Transactional
  // 플레이리스트 삭제 및 캐시 무효화
  public void removePlaylist(UUID playlistId, UUID userId) {
    validatePlaylistAndUserIds(playlistId, userId);

    Playlist playlist = findPlaylistOrThrow(playlistId);
    assertOwner(playlist, userId);
    playlistRepository.deleteById(playlistId);
    log.info("플레이리스트 삭제 완료 playlistId={} userId={}", playlistId, userId);
    // 커밋 후 콘텐츠 캐시 삭제
    runAfterCommit(
        () -> {
          deletePlaylistContentCaches(playlistId);
        });
  }

  @Transactional
  // 플레이리스트 기본 정보 수정
  public PlaylistDto updatePlaylist(
      UUID playlistId, @Valid PlaylistUpdateRequest request, UUID userId) {
    validatePlaylistAndUserIds(playlistId, userId);

    Playlist playlist = findPlaylistOrThrow(playlistId);
    assertOwner(playlist, userId);

    playlist.update(request.getTitle(), request.getDescription());

    return playlistQueryService.findPlaylist(playlistId, userId);
  }

  // 플레이리스트 콘텐츠 관련 캐시 삭제
  private void deletePlaylistContentCaches(UUID playlistId) {
    try {
      redisTemplate.delete(RedisKeyPrefix.PLAYLIST_CONTENTS + playlistId);
      redisTemplate.delete(RedisKeyPrefix.PLAYLIST_THUMBNAIL_CONTENT + playlistId);
    } catch (Exception e) {
      log.warn("레디스 캐시 삭제 실패 playlistId={} error={}", playlistId, e.getMessage());
    }
  }

  // 트랜잭션 커밋 이후에만 캐시 작업을 실행
  private void runAfterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
    } else {
      action.run();
    }
  }

  // 플레이리스트와 사용자 식별자 검증
  private void validatePlaylistAndUserIds(UUID playlistId, UUID userId) {
    if (userId == null || playlistId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
  }

  // 플레이리스트 존재 여부 확인
  private void assertPlaylistExists(UUID playlistId) {
    if (!playlistRepository.existsById(playlistId)) {
      throw new BusinessException(CommonErrorCode.NOT_FOUND);
    }
  }

  // 플레이리스트-콘텐츠 입력값 검증
  private void validatePlaylistContentInputs(UUID playlistId, UUID contentId, UUID userId) {
    if (userId == null || playlistId == null || contentId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
  }

  // 플레이리스트 조회 후 없으면 예외
  private Playlist findPlaylistOrThrow(UUID playlistId) {
    return playlistRepository
        .findById(playlistId)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
  }

  // 플레이리스트 소유자 검증
  private void assertOwner(Playlist playlist, UUID userId) {
    if (!playlist.getOwnerId().equals(userId)) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }
  }
}
