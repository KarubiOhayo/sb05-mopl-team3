package io.mopl.api.watching.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID> {}
