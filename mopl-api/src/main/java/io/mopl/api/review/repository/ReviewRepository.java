package io.mopl.api.review.repository;

import io.mopl.api.review.domain.Review;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

  /**
 * Retrieves a page of reviews for the given content identifier.
 *
 * @param contentId the UUID of the content whose reviews to retrieve
 * @param pageable  pagination and sorting parameters for the returned page
 * @return a page of Review objects for the specified content, possibly empty
 */
Page<Review> findByContentId(UUID contentId, Pageable pageable);

  /**
 * Checks whether a review exists for the specified content and author.
 *
 * @param contentId the UUID of the content to check
 * @param authorId  the UUID of the author to check
 * @return `true` if at least one Review exists matching both `contentId` and `authorId`, `false` otherwise
 */
boolean existsByContentIdAndAuthorId(UUID contentId, UUID authorId);
}