package io.mopl.api.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import io.mopl.api.review.domain.Review;
import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.review.mapper.ReviewMapper;
import io.mopl.api.review.repository.ReviewRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @InjectMocks private ReviewService reviewService;

  @Mock private ReviewRepository reviewRepository;

  @Mock private ReviewMapper reviewMapper;

  @Mock private UserService userService;

  @Test
  @DisplayName("리뷰 생성 성공")
  void create_Success() {
    // given
    UUID userId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest();
    request.setContentId(UUID.randomUUID());
    request.setText("리뷰");
    request.setRating(5.0);

    Review review = Review.builder().id(UUID.randomUUID()).authorId(userId).build();
    UserSummary userSummary = UserSummary.builder().userId(userId).name("테스터").build();
    ReviewDto reviewDto = ReviewDto.builder().id(review.getId()).build();

    given(reviewRepository.existsByContentIdAndAuthorId(any(), any())).willReturn(false);
    given(reviewMapper.toEntity(any(), any())).willReturn(review);
    given(reviewRepository.save(any())).willReturn(review);
    given(userService.getUserSummary(any())).willReturn(userSummary);
    given(reviewMapper.toDto(any(), any())).willReturn(reviewDto);

    // when
    ReviewDto result = reviewService.create(request, userId);

    // then
    assertThat(result).isNotNull();
    verify(reviewRepository).save(any());
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 이미 존재함")
  void create_Fail_AlreadyExists() {
    // given
    UUID userId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest();
    request.setContentId(UUID.randomUUID());

    given(reviewRepository.existsByContentIdAndAuthorId(any(), any())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> reviewService.create(request, userId))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("리뷰 단건 조회 성공")
  void findById_Success() {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = Review.builder().id(reviewId).authorId(userId).build();
    UserSummary userSummary = UserSummary.builder().userId(userId).build();
    ReviewDto reviewDto = ReviewDto.builder().id(reviewId).build();

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(userService.getUserSummary(userId)).willReturn(userSummary);
    given(reviewMapper.toDto(review, userSummary)).willReturn(reviewDto);

    // when
    ReviewDto result = reviewService.findById(reviewId, null);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(reviewId);
  }

  @Test
  @DisplayName("콘텐츠별 리뷰 목록 조회 성공")
  void findByContentId_Success() {
    // given
    UUID contentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = Review.builder().id(UUID.randomUUID()).authorId(userId).build();
    List<Review> reviews = List.of(review);
    UserSummary userSummary = UserSummary.builder().userId(userId).build();
    ReviewDto reviewDto = ReviewDto.builder().id(review.getId()).build();

    given(reviewRepository.findByContentId(contentId)).willReturn(reviews);
    given(userService.getUserSummary(userId)).willReturn(userSummary);
    given(reviewMapper.toDto(review, userSummary)).willReturn(reviewDto);

    // when
    List<ReviewDto> result = reviewService.findByContentId(contentId, null);

    // then
    assertThat(result).hasSize(1);
  }
}
