package io.mopl.api.playlist.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistContentRepository
    extends JpaRepository<PlaylistContent, PlaylistContentId> {}
