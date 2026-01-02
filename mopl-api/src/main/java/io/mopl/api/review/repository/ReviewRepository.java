package io.mopl.api.review.repository;

import io.mopl.api.review.domain.Review;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

  Page<Review> findByContentId(UUID contentId, Pageable pageable);

  boolean existsByContentIdAndAuthorId(UUID contentId, UUID authorId);
}
