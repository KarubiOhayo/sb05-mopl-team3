package io.mopl.api.playlist.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistContentRepository
    extends JpaRepository<PlaylistContent, PlaylistContentId> {

  @Modifying
  @Query(
      value =
          "insert into playlist_contents (playlist_id, content_id) "
              + "values (:playlistId, :contentId) "
              + "on duplicate key update playlist_id = playlist_id",
      nativeQuery = true)
  int insertIgnore(@Param("playlistId") String playlistId, @Param("contentId") String contentId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from PlaylistContent pc where pc.id.contentId = :contentId")
  void deleteByIdContentId(@Param("contentId") UUID contentId);
}
