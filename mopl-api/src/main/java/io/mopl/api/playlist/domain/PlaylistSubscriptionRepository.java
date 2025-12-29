package io.mopl.api.playlist.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistSubscriptionRepository
    extends JpaRepository<PlaylistSubscription, PlaylistSubscriptionId> {}
