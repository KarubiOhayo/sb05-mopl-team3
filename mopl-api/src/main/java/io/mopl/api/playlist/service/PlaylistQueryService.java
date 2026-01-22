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
import io.mopl.redis.constants.RedisKeyPrefix;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
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
  private final RedisTemplate<String, String> redisTemplate;

  // 플레이리스트 목록 조회
  public CursorResponse<PlaylistDto> findPlaylists(PlaylistSearchRequest request, UUID me) {

    // 정렬 기본값
    String sortBy = request.getSortByOrDefault();
    String sortDirection = request.getSortDirectionOrDefault();

    // 요청 객체 그대로 리포지토리 전달
    PlaylistPage page = playlistQueryRepository.findPlaylistsPage(request);

    // totalCount는 동일 필터 조건으로 계산
    long totalCount = getTotalCount(request);

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
        playlistContentLoader.loadThumbnailContentsByPlaylistIds(playlistIds);

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

  private long getTotalCount(PlaylistSearchRequest request) {
    String cacheKey = buildTotalCountCacheKey(request);
    try {
      String cached = redisTemplate.opsForValue().get(cacheKey);
      if (cached != null) {
        try {
          return Long.parseLong(cached);
        } catch (NumberFormatException e) {
          log.warn("Redis 캐시 값 파싱 실패 key={} value={}", cacheKey, cached);
        }
      }
    } catch (Exception e) {
      log.warn("Redis 캐시 조회 실패 key={} error={}", cacheKey, e.getMessage());
    }

    long totalCount = playlistQueryRepository.countPlaylists(request);

    try {
      redisTemplate.opsForValue().set(cacheKey, String.valueOf(totalCount), Duration.ofMinutes(5));
    } catch (Exception e) {
      log.warn("Redis 캐시 저장 실패 key={} error={}", cacheKey, e.getMessage());
    }

    return totalCount;
  }

  private String buildTotalCountCacheKey(PlaylistSearchRequest request) {
    String raw =
        String.join(
            "|",
            nullToEmpty(request.getKeywordLike()),
            String.valueOf(request.getOwnerIdEqual()),
            String.valueOf(request.getSubscriberIdEqual()));
    return RedisKeyPrefix.PLAYLIST_COUNT + sha256Hex(raw);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String sha256Hex(String value) {
    return Integer.toHexString(value.hashCode());
  }

  // 플레이리스트 단건 조회
  public PlaylistDto findPlaylist(UUID playlistId, UUID me) {
    Playlist playlist =
        playlistRepository
            .findById(playlistId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));

    Map<UUID, UserSummary> ownerMap = playlistOwnerLoader.loadOwners(Set.of(playlist.getOwnerId()));
    UserSummary owner = ownerMap.get(playlist.getOwnerId());
    if (owner == null) {
      owner = userService.getUserSummary(playlist.getOwnerId());
    }

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
