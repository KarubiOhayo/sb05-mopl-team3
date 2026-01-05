package io.mopl.api.review.controller;

import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Review", description = "리뷰 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "리뷰 생성", description = "새로운 리뷰를 작성합니다.")
  @PostMapping
  public ResponseEntity<ReviewDto> createReview(
      @Valid @RequestBody ReviewCreateRequest request, @AuthenticationPrincipal UUID userId) {
    ReviewDto reviewDto = reviewService.create(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(reviewDto);
  }

  @Operation(summary = "리뷰 단건 조회", description = "특정 리뷰를 단건 조회합니다.")
  @GetMapping("/{reviewId}")
  public ResponseEntity<ReviewDto> getReview(@PathVariable UUID reviewId) {
    ReviewDto reviewDto = reviewService.findById(reviewId);
    return ResponseEntity.ok(reviewDto);
  }

  @Operation(summary = "콘텐츠별 리뷰 목록 조회", description = "특정 콘텐츠에 달린 리뷰 목록을 조회합니다.")
  @GetMapping
  public ResponseEntity<List<ReviewDto>> getReviews(@RequestParam UUID contentId) {
    List<ReviewDto> reviews = reviewService.findByContentId(contentId);
    return ResponseEntity.ok(reviews);
  }
}
