package io.mopl.api.review.repository;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.review.dto.ReviewCursorRequest;
import io.mopl.api.review.dto.ReviewDto;
import java.util.UUID;

public interface ReviewQueryRepository {
  CursorResponse<ReviewDto> findReviewsPage(UUID contentId, ReviewCursorRequest cursorRequest);

  long countReviews(UUID contentId);
}
