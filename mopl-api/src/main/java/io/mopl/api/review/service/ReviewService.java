package io.mopl.api.review.service;

import io.mopl.api.review.domain.Review;
import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.review.error.ReviewErrorCode;
import io.mopl.api.review.event.ReviewEventPublisher;
import io.mopl.api.review.mapper.ReviewMapper;
import io.mopl.api.review.repository.ReviewRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.core.event.review.ReviewCreatedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final ReviewEventPublisher reviewEventPublisher;

  // private final UserService userService; // UserService 구현 전까지 주석 처리

  // 1. 리뷰 목록 조회

  // 2. 리뷰 생성
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

    reviewEventPublisher.publish(
        new ReviewCreatedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            savedReview.getId().toString(),
            savedReview.getContentId().toString(),
            savedReview.getRating()));
    log.info(
        "리뷰 이벤트 발행: reviewId={}, contentId={}, rating={}",
        savedReview.getId(),
        savedReview.getContentId(),
        savedReview.getRating());

    // 사용자 정보 조회 (임시로 null 처리)
    // UserSummary author = userService.getSummary(authorId);
    UserSummary author = null;

    // entity를 dto로 전환 (사용자 응답 생성)
    return reviewMapper.toDto(savedReview, author);
  }

  // 3. 리뷰 삭제

  // 4. 리뷰 수정

}
