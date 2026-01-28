package io.mopl.api.review.service;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.content.service.ContentThumbnailUploadService;
import io.mopl.api.review.domain.Review;
import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewCursorRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.review.dto.ReviewUpdateRequest;
import io.mopl.api.review.error.ReviewErrorCode;
import io.mopl.api.review.event.ReviewEventPublisher;
import io.mopl.api.review.mapper.ReviewMapper;
import io.mopl.api.review.repository.ReviewQueryRepository;
import io.mopl.api.review.repository.ReviewRepository;
import io.mopl.api.review.service.cache.ReviewCacheService;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.core.event.review.ReviewCreatedEvent;
import io.mopl.core.event.review.ReviewDeletedEvent;
import io.mopl.core.event.review.ReviewUpdatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewQueryRepository reviewQueryRepository;
  private final ReviewMapper reviewMapper;
  private final ReviewEventPublisher reviewEventPublisher;

  // private final UserService userService; // UserService 구현 전까지 주석 처리
  private final UserService userService;
  private final UserRepository userRepository;
  private final ReviewCacheService reviewCacheService;
  private final ContentThumbnailUploadService contentThumbnailUploadService;

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
    log.info(
        "리뷰 등록 완료: reviewId={}, contentId={}, rating={}",
        savedReview.getId(),
        savedReview.getContentId(),
        savedReview.getRating());

    runAfterCommit(
        () -> {
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
        });
    UserSummary author = userService.getUserSummary(authorId);

    reviewCacheService.evictFirstPage(savedReview.getContentId());

    return reviewMapper.toDto(savedReview, author);
  }

  @Transactional(readOnly = true)
  public ReviewDto findById(UUID reviewId) {
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
            .map(user -> new UserSummary(user.getId(), user.getName(), user.getProfileImageKey()))
            .collect(Collectors.toMap(UserSummary::getUserId, Function.identity()));

    return reviews.stream()
        .map(
            review -> {
              UserSummary author = authorMap.get(review.getAuthorId());
              if (author == null) {
                author = new UserSummary(review.getAuthorId(), "Unknown", null);
              }
              return reviewMapper.toDto(review, author);
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public CursorResponse<ReviewDto> getReviews(UUID contentId, ReviewCursorRequest request) {
    CursorResponse<ReviewDto> cached = reviewCacheService.getFirstPage(contentId, request);
    if (cached != null) {
      return cached;
    }

    // 1. QueryRepository를 통해 페이징된 ReviewDto 조회 (DTO 직접 조회 + Join)
    CursorResponse<ReviewDto> response = reviewQueryRepository.findReviewsPage(contentId, request);

    // 2. 프로필 이미지 URL 변환 (S3 Key -> Presigned URL)
    List<ReviewDto> processedDtos =
        response.getData().stream()
            .map(
                dto -> {
                  UserSummary author = dto.getAuthor();
                  String presignedUrl =
                      contentThumbnailUploadService.generatePresignedUrl(
                          author.getProfileImageUrl()); // 현재 DTO에는 Key가 들어있음

                  UserSummary newAuthor =
                      new UserSummary(author.getUserId(), author.getName(), presignedUrl);

                  return new ReviewDto(
                      dto.getId(),
                      dto.getContentId(),
                      newAuthor,
                      dto.getText(),
                      dto.getRating(),
                      dto.getCreatedAt());
                })
            .toList();

    CursorResponse<ReviewDto> processedResponse =
        CursorResponse.<ReviewDto>builder()
            .data(processedDtos)
            .nextCursor(response.getNextCursor())
            .nextIdAfter(response.getNextIdAfter())
            .hasNext(response.isHasNext())
            .totalCount(response.getTotalCount())
            .sortBy(response.getSortBy())
            .sortDirection(response.getSortDirection())
            .build();

    reviewCacheService.cacheFirstPage(contentId, request, processedResponse);

    return processedResponse;
  }

  @Transactional
  public void delete(UUID reviewId, UUID authorId) {
    // 1. 존재 확인
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new BusinessException(ReviewErrorCode.NOT_FOUND_REVIEW));

    // 2. 작성자 확인
    // 리뷰 삭제 권한 = 리뷰 작성한 본인만!
    if (!review.getAuthorId().equals(authorId)) {
      throw new BusinessException(ReviewErrorCode.NOT_AUTHOR);
    }
    log.info(
        "리뷰 삭제 동작: reviewId={}, contentId={}, rating={}",
        review.getId(),
        review.getContentId(),
        review.getRating());
    // 3. 삭제
    reviewRepository.delete(review);

    runAfterCommit(
        () -> {
          reviewEventPublisher.publish(
              new ReviewDeletedEvent(
                  UUID.randomUUID().toString(),
                  Instant.now(),
                  review.getId().toString(),
                  review.getContentId().toString(),
                  review.getRating()));
          log.info(
              "리뷰 삭제 이벤트 발행: reviewId={}, contentId={}, rating={}",
              review.getId(),
              review.getContentId(),
              review.getRating());
        });

    reviewCacheService.evictFirstPage(review.getContentId());
  }

  @Transactional
  public ReviewDto update(UUID reviewId, ReviewUpdateRequest request, UUID authorId) {
    // 1. 존재 확인
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new BusinessException(ReviewErrorCode.NOT_FOUND_REVIEW));

    if (!review.getAuthorId().equals(authorId)) {
      throw new BusinessException(ReviewErrorCode.NOT_AUTHOR);
    }

    double beforeRating = review.getRating();
    Double rating = request.getRating();
    double safeRating = rating == null ? beforeRating : rating;
    review.update(request.getText(), safeRating);
    log.info(
        "리뷰 수정 완료: reviewId={}, contentId={}, rating={}",
        review.getId(),
        review.getContentId(),
        review.getRating());

    if (Double.compare(beforeRating, safeRating) != 0) {
      runAfterCommit(
          () -> {
            reviewEventPublisher.publish(
                new ReviewUpdatedEvent(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    review.getId().toString(),
                    review.getContentId().toString(),
                    beforeRating,
                    safeRating));
            log.info(
                "리뷰 업데이트 이벤트 발행: reviewId={}, contentId={}, beforeRating={}, afterRating={}",
                review.getId(),
                review.getContentId(),
                beforeRating,
                safeRating);
          });
    }

    UserSummary author = userService.getUserSummary(authorId);

    ReviewDto dto = reviewMapper.toDto(review, author);

    reviewCacheService.evictFirstPage(review.getContentId());

    // 4. 응답 (dirty checking으로 자동 저장됨)
    return dto;
  }

  // 트랜잭션 커밋 이후에만 캐시 작업을 실행
  private void runAfterCommit(Runnable action) {
    Runnable safeAction =
        () -> {
          try {
            action.run();
          } catch (Exception e) {
            log.error("afterCommit 작업 실패", e);
          }
        };

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              safeAction.run();
            }
          });
    } else {
      safeAction.run();
    }
  }
}
