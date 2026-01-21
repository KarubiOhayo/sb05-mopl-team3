package io.mopl.api.review.controller;

import io.mopl.api.common.config.AuthUser;
import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewCursorRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.review.dto.ReviewUpdateRequest;
import io.mopl.api.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
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
      @Valid @RequestBody ReviewCreateRequest request, @AuthenticationPrincipal AuthUser authUser) {
    ReviewDto reviewDto = reviewService.create(request, authUser.getUserId());
    return ResponseEntity.status(HttpStatus.CREATED).body(reviewDto);
  }

  @Operation(summary = "리뷰 목록 조회(커서 페이지네이션)", description = "리뷰 목록을 페이지네이션하여 조회합니다.")
  @GetMapping
  public ResponseEntity<CursorResponse<ReviewDto>> getReviews(
      @Parameter(description = "콘텐츠 ID", required = true) @RequestParam UUID contentId,
      @ModelAttribute ReviewCursorRequest request) {
    // Service 계층으로 로직 위임
    CursorResponse<ReviewDto> response = reviewService.getReviews(contentId, request);

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "리뷰 단건 조회", description = "특정 리뷰를 단건 조회합니다.")
  @GetMapping("/{reviewId}")
  public ResponseEntity<ReviewDto> getReview(@PathVariable UUID reviewId) {
    ReviewDto reviewDto = reviewService.findById(reviewId);
    return ResponseEntity.ok(reviewDto);
  }

  @Operation(summary = "리뷰 삭제", description = "리뷰를 삭제합니다.")
  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(
      @PathVariable UUID reviewId, @AuthenticationPrincipal AuthUser authUser) {
    reviewService.delete(reviewId, authUser.getUserId());
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "리뷰 수정", description = "리뷰를 수정합니다.")
  @PatchMapping("/{reviewId}")
  public ResponseEntity<ReviewDto> updateReview(
      @PathVariable UUID reviewId,
      @Valid @RequestBody ReviewUpdateRequest request,
      @AuthenticationPrincipal AuthUser authUser) {
    ReviewDto reviewDto = reviewService.update(reviewId, request, authUser.getUserId());
    return ResponseEntity.ok(reviewDto);
  }
}
