package io.mopl.api.playlist.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

  @Modifying
  @Query("update Playlist p set p.subscriberCount = p.subscriberCount + 1 where p.id = :id")
  int increaseSubscriberCount(@Param("id") UUID id);

  @Modifying
  @Query(
      "update Playlist p set p.subscriberCount = p.subscriberCount - 1 where p.id = :id and p.subscriberCount > 0")
  int decreaseSubscriberCount(@Param("id") UUID id);
}
