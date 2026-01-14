package io.mopl.api.review.service;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.review.domain.Review;
import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewCursorRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.review.error.ReviewErrorCode;
import io.mopl.api.review.mapper.ReviewMapper;
import io.mopl.api.review.repository.ReviewQueryRepository;
import io.mopl.api.review.repository.ReviewRepository;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewQueryRepository reviewQueryRepository;
  private final ReviewMapper reviewMapper;
  private final UserService userService;
  private final UserRepository userRepository;

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
  public List<ReviewDto> findByContentId(UUID contentId) {
    // 1. 리뷰 목록 조회 (쿼리 1번)
    List<Review> reviews = reviewRepository.findByContentId(contentId);

    if (reviews.isEmpty()) {
      return List.of();
    }

    // 2. 작성자 ID 추출 (중복 제거)
    List<UUID> authorsIds = reviews.stream().map(Review::getAuthorId).distinct().toList();

    // 3. UserRepository 표준 메소드로 유저 정보 일괄 조회(쿼리 1번)
    // 내부적으로 "WHERE id IN (...)" 쿼리를 실행
    List<User> users = userRepository.findAllById(authorsIds);

    Map<UUID, UserSummary> authorMap =
        users.stream()
            .map(user -> new UserSummary(user.getId(), user.getName(), user.getProfileImageUrl()))
            .collect(Collectors.toMap(UserSummary::getUserId, Function.identity()));

    return reviews.stream()
        .map(
            review -> {
              UserSummary author = authorMap.get(review.getAuthorId());
              return reviewMapper.toDto(review, author);
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public CursorResponse<ReviewDto> getReviews(UUID contentId, ReviewCursorRequest request) {
    // 1. QueryRepository를 통해 페이징된 리뷰 엔티티 조회
    CursorResponse<Review> entityResponse =
        reviewQueryRepository.findReviewsPage(contentId, request);

    // 2. 조회된 리뷰들에서 작성자 ID 추출
    List<UUID> authorIds =
        entityResponse.getData().stream().map(Review::getAuthorId).distinct().toList();

    // 3. 작성자 정보 일괄 조회 (N+1 문제 방지)
    Map<UUID, UserSummary> authorMap =
        userRepository.findAllById(authorIds).stream()
            .map(user -> new UserSummary(user.getId(), user.getName(), user.getProfileImageUrl()))
            .collect(Collectors.toMap(UserSummary::getUserId, Function.identity()));

    // 4. 엔티티 -> DTO 변환 (작성자 정보 매핑 포함)
    List<ReviewDto> dtos =
        entityResponse.getData().stream()
            .map(
                review -> {
                  UserSummary author = authorMap.get(review.getAuthorId());
                  return reviewMapper.toDto(review, author);
                })
            .toList();

    // 5. CursorResponse<ReviewDto> 생성 및 반환
    return CursorResponse.<ReviewDto>builder()
        .data(dtos)
        .nextCursor(entityResponse.getNextCursor())
        .nextIdAfter(entityResponse.getNextIdAfter())
        .hasNext(entityResponse.isHasNext())
        .totalCount(entityResponse.getTotalCount())
        .sortBy(entityResponse.getSortBy())
        .sortDirection(entityResponse.getSortDirection())
        .build();
  }
}
