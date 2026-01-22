package io.mopl.api.playlist.service;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.common.dto.SortDirection;
import io.mopl.api.content.dto.ContentSummary;
import io.mopl.api.playlist.domain.Playlist;
import io.mopl.api.playlist.domain.PlaylistQueryRepository;
import io.mopl.api.playlist.domain.PlaylistRepository;
import io.mopl.api.playlist.domain.PlaylistSubscriptionId;
import io.mopl.api.playlist.domain.PlaylistSubscriptionRepository;
import io.mopl.api.playlist.dto.PlaylistDto;
import io.mopl.api.playlist.dto.PlaylistPage;
import io.mopl.api.playlist.dto.PlaylistSearchRequest;
import io.mopl.api.playlist.service.loader.PlaylistContentLoader;
import io.mopl.api.playlist.service.loader.PlaylistOwnerLoader;
import io.mopl.api.playlist.service.loader.PlaylistSubscriptionLoader;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistQueryService {

  private final PlaylistQueryRepository playlistQueryRepository;
  private final PlaylistOwnerLoader playlistOwnerLoader;
  private final PlaylistSubscriptionLoader playlistSubscriptionLoader;
  private final PlaylistContentLoader playlistContentLoader;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final PlaylistRepository playlistRepository;
  private final UserService userService;

  // 플레이리스트 목록 조회
  public CursorResponse<PlaylistDto> findPlaylists(PlaylistSearchRequest request, UUID me) {

    // 정렬 기본값
    String sortBy = request.getSortByOrDefault();
    String sortDirection = request.getSortDirectionOrDefault();

    // 요청 객체 그대로 리포지토리 전달
    PlaylistPage page = playlistQueryRepository.findPlaylistsPage(request);

    // totalCount는 동일 필터 조건으로 계산
    long totalCount = playlistQueryRepository.countPlaylists(request);

    List<Playlist> playlists = page.getPlaylists();

    // ownerId, playlistId 수집 (배치 로딩용)
    Set<UUID> ownerIds = new HashSet<>();
    List<UUID> playlistIds = new ArrayList<>();
    for (Playlist playlist : playlists) {
      ownerIds.add(playlist.getOwnerId());
      playlistIds.add(playlist.getId());
    }

    // 연관 데이터 일괄 로딩
    Map<UUID, UserSummary> ownerMap = playlistOwnerLoader.loadOwners(ownerIds);
    Set<UUID> subscribedPlaylistIds =
        playlistSubscriptionLoader.loadSubscribedPlaylistIdsByMe(me, playlistIds);
    Map<UUID, List<ContentSummary>> contentsMap =
        playlistContentLoader.loadContentsByPlaylistIds(playlistIds);

    // 응답 DTO 조립
    List<PlaylistDto> data = new ArrayList<>();
    for (Playlist playlist : playlists) {
      UUID playlistId = playlist.getId();

      UserSummary owner = ownerMap.get(playlist.getOwnerId());
      boolean subscribedByMe = subscribedPlaylistIds.contains(playlistId);

      List<ContentSummary> contents = contentsMap.get(playlistId);
      if (contents == null) {
        contents = List.of();
      }

      PlaylistDto dto =
          PlaylistDto.builder()
              .id(playlistId)
              .owner(owner)
              .title(playlist.getTitle())
              .description(playlist.getDescription())
              .updatedAt(playlist.getUpdatedAt())
              .subscriberCount(playlist.getSubscriberCount())
              .subscribedByMe(subscribedByMe)
              .contents(contents)
              .build();

      data.add(dto);
    }

    return CursorResponse.<PlaylistDto>builder()
        .data(data)
        .nextCursor(page.getNextCursor())
        .nextIdAfter(page.getNextIdAfter())
        .hasNext(page.isHasNext())
        .totalCount(totalCount)
        .sortBy(sortBy)
        .sortDirection(parseSortDirection(sortDirection))
        .build();
  }

  private SortDirection parseSortDirection(String raw) {
    try {
      return SortDirection.valueOf(raw);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "Invalid sortDirection. Use ASCENDING or DESCENDING.");
    }
  }

  // 플레이리스트 단건 조회
  public PlaylistDto findPlaylist(UUID playlistId, UUID me) {
    Playlist playlist =
        playlistRepository
            .findById(playlistId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));

    UserSummary owner = userService.getUserSummary(playlist.getOwnerId());

    boolean subscribedByMe = false;
    if (me != null) {
      PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, me);
      subscribedByMe = playlistSubscriptionRepository.existsById(id);
    }

    List<ContentSummary> contents = playlistContentLoader.loadContentsByPlaylistId(playlistId);

    return PlaylistDto.builder()
        .id(playlistId)
        .owner(owner)
        .title(playlist.getTitle())
        .description(playlist.getDescription())
        .updatedAt(playlist.getUpdatedAt())
        .subscriberCount(playlist.getSubscriberCount())
        .subscribedByMe(subscribedByMe)
        .contents(contents)
        .build();
  }
}
