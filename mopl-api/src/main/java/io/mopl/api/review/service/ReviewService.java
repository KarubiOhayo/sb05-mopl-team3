package io.mopl.api.review.service;

import io.mopl.api.review.domain.Review;
import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.review.error.ReviewErrorCode;
import io.mopl.api.review.mapper.ReviewMapper;
import io.mopl.api.review.repository.ReviewRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;

  // private final UserService userService; // UserService 구현 전까지 주석 처리

  // 1. 리뷰 목록 조회

  /**
   * Create a new review for the specified content and author.
   *
   * Validates the author ID and that the author has not already created a review for the same content,
   * persists the new review, and returns a DTO of the saved review.
   *
   * @param request  the review creation request payload
   * @param authorId the UUID of the author creating the review
   * @return the created ReviewDto representing the saved review
   * @throws BusinessException if {@code authorId} is null or a review by the same author for the content already exists
   */
  @Transactional
  public ReviewDto create(ReviewCreateRequest request, UUID authorId) {

    if (authorId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    if (reviewRepository.existsByContentIdAndAuthorId(request.getContentId(), authorId)) {
      throw new BusinessException(ReviewErrorCode.ALREADY_EXISTS_REVIEW);
    }

    // dto를 entity로 전환 (요청 처리)
    Review review = reviewMapper.toEntity(request, authorId);
    Review savedReview = reviewRepository.save(review);

    // 사용자 정보 조회 (임시로 null 처리)
    // UserSummary author = userService.getSummary(authorId);
    UserSummary author = null;

    // entity를 dto로 전환 (사용자 응답 생성)
    return reviewMapper.toDto(savedReview, author);
  }

  // 3. 리뷰 삭제

  // 4. 리뷰 수정

}