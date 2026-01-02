package io.mopl.api.playlist.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.mopl.api.playlist.domain.Playlist;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {}
