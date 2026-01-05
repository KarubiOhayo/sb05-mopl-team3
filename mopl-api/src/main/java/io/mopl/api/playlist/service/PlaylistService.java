package io.mopl.api.playlist.service;

import io.mopl.api.playlist.domain.Playlist;
import io.mopl.api.playlist.domain.PlaylistSubscription;
import io.mopl.api.playlist.domain.PlaylistSubscriptionId;
import io.mopl.api.playlist.dto.PlaylistCreateRequest;
import io.mopl.api.playlist.dto.PlaylistDto;
import io.mopl.api.playlist.repository.PlaylistRepository;
import io.mopl.api.playlist.repository.PlaylistSubscriptionRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserService userService;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;

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

  @Transactional
  public void subscribe(UUID playlistId, UUID userId) {
    if (userId == null || playlistId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    Playlist playlist =
        playlistRepository
            .findById(playlistId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));

    PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, userId);
    if (playlistSubscriptionRepository.existsById(id)) {
      throw new BusinessException(CommonErrorCode.CONFLICT);
    }

    PlaylistSubscription subscription = PlaylistSubscription.builder().id(id).build();
    playlistSubscriptionRepository.save(subscription);

    // subscriberCount 증가
    playlist.increaseSubscriberCount();
    playlistRepository.save(playlist);
  }

  @Transactional
  public void unsubscribe(UUID playlistId, UUID userId) {
    if (userId == null || playlistId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    Playlist playlist =
        playlistRepository
            .findById(playlistId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));

    PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, userId);
    if (!playlistSubscriptionRepository.existsById(id)) {
      throw new BusinessException(CommonErrorCode.CONFLICT);
    }

    playlistSubscriptionRepository.deleteById(id);

    playlist.decreaseSubscriberCount();
    playlistRepository.save(playlist);
  }
}
