package io.mopl.api.playlist.repository;

import io.mopl.api.playlist.domain.PlaylistContent;
import io.mopl.api.playlist.domain.PlaylistContentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistContentRepository
    extends JpaRepository<PlaylistContent, PlaylistContentId> {}
