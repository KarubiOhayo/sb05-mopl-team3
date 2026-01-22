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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

  // 플레이리스트 목록 조회 및 응답 조립
  public CursorResponse<PlaylistDto> findPlaylists(PlaylistSearchRequest request, UUID me) {

    // 정렬 기본값 결정
    String sortBy = request.getSortByOrDefault();
    String sortDirection = request.getSortDirectionOrDefault();

    // 페이지네이션 조회
    PlaylistPage page = playlistQueryRepository.findPlaylistsPage(request);

    // 동일 필터 조건으로 totalCount 계산(캐시 포함)
    long totalCount = getTotalCount(request);

    List<Playlist> playlists = page.getPlaylists();

    // ownerId, playlistId 수집(배치 로딩용)
    Set<UUID> ownerIds = new HashSet<>();
    List<UUID> playlistIds = new ArrayList<>();
    for (Playlist playlist : playlists) {
      ownerIds.add(playlist.getOwnerId());
      playlistIds.add(playlist.getId());
    }

    // 관련 데이터 배치 로딩
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

  // 정렬 방향 문자열 파싱
  private SortDirection parseSortDirection(String raw) {
    try {
      return SortDirection.valueOf(raw);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "Invalid sortDirection. Use ASCENDING or DESCENDING.");
    }
  }

  // totalCount 캐시 조회 후 없으면 계산
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

  // totalCount 캐시 키 생성
  private String buildTotalCountCacheKey(PlaylistSearchRequest request) {
    String keyword = nullToEmpty(request.getKeywordLike());
    String raw =
        keyword.length()
            + ":"
            + keyword
            + "|"
            + String.valueOf(request.getOwnerIdEqual())
            + "|"
            + String.valueOf(request.getSubscriberIdEqual());
    return RedisKeyPrefix.PLAYLIST_COUNT + sha256Hex(raw);
  }

  // null 안전 처리
  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static final ThreadLocal<MessageDigest> SHA256_DIGEST =
      ThreadLocal.withInitial(
          () -> {
            try {
              return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
              throw new IllegalStateException("SHA-256 not available", e);
            }
          });

  // SHA-256 해시를 16진 문자열로 변환
  private String sha256Hex(String value) {
    MessageDigest digest = SHA256_DIGEST.get();
    digest.reset();
    byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder hexString = new StringBuilder(hash.length * 2);
    for (byte b : hash) {
      hexString.append(String.format("%02x", b));
    }
    return hexString.toString();
  }

  // 플레이리스트 상세 조회
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
