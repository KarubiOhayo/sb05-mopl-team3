package io.mopl.api.playlist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.mopl.api.playlist.domain.PlaylistContent;
import io.mopl.api.playlist.domain.PlaylistContentId;

@Repository
public interface PlaylistContentRepository
    extends JpaRepository<PlaylistContent, PlaylistContentId> {}
