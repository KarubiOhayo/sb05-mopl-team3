package io.mopl.api.review.repository;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.review.domain.Review;
import io.mopl.api.review.dto.ReviewCursorRequest;
import java.util.UUID;

public interface ReviewQueryRepository {

  CursorResponse<Review> findReviewsPage(UUID contentId, ReviewCursorRequest cursorRequest);

  long countReviews(UUID contentId);
}
