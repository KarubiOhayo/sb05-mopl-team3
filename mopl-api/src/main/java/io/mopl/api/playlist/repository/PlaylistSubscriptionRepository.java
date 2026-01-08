package io.mopl.api.playlist.repository;

import io.mopl.api.playlist.domain.PlaylistSubscription;
import io.mopl.api.playlist.domain.PlaylistSubscriptionId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistSubscriptionRepository
    extends JpaRepository<PlaylistSubscription, PlaylistSubscriptionId> {

  /**
   * 구독한 플레이리스트 중에서, 현재 조회된 playlistIds 목록에 포함되는 것만 한 번에 조회한다. SQL 느낌: SELECT * FROM
   * playlist_subscriptions WHERE user_id = :userId AND playlist_id IN (:playlistIds)
   */
  List<PlaylistSubscription> findByIdUserIdAndIdPlaylistIdIn(
      UUID userId, Collection<UUID> playlistIds);
}
