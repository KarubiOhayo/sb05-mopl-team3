package io.mopl.api.review.service;

import io.mopl.api.review.domain.Review;
import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.review.error.ReviewErrorCode;
import io.mopl.api.review.mapper.ReviewMapper;
import io.mopl.api.review.repository.ReviewRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final UserService userService;

  @Transactional
  public ReviewDto create(ReviewCreateRequest request, UUID authorId) {

    if (authorId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    if (reviewRepository.existsByContentIdAndAuthorId(request.getContentId(), authorId)) {
      throw new BusinessException(ReviewErrorCode.ALREADY_EXISTS_REVIEW);
    }

    Review review = reviewMapper.toEntity(request, authorId);
    Review savedReview = reviewRepository.save(review);

    UserSummary author = userService.getUserSummary(authorId);

    return reviewMapper.toDto(savedReview, author);
  }

  @Transactional(readOnly = true)
  public ReviewDto findById(UUID reviewId, UUID userId) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new BusinessException(ReviewErrorCode.NOT_FOUND_REVIEW));

    UserSummary author = userService.getUserSummary(review.getAuthorId());

    return reviewMapper.toDto(review, author);
  }

  @Transactional(readOnly = true)
  public List<ReviewDto> findByContentId(UUID contentId, UUID userId) {
    List<Review> reviews = reviewRepository.findByContentId(contentId);
    return reviews.stream()
        .map(
            review -> {
              UserSummary author = userService.getUserSummary(review.getAuthorId());
              return reviewMapper.toDto(review, author);
            })
        .toList();
  }
}
