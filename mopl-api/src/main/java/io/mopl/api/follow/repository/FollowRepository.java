package io.mopl.api.follow.repository;

import io.mopl.api.follow.domain.Follow;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

  Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
}
