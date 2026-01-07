package io.mopl.api.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mopl.api.common.config.AuthUser;
import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.review.service.ReviewService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private ReviewService reviewService;

  @Test
  @DisplayName("리뷰 생성 성공")
  @WithMockUser
  void createReview_Success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    AuthUser authUser =
        AuthUser.builder().userId(userId).email("test@test.com").role("USER").build();
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(authUser, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    ReviewCreateRequest request = new ReviewCreateRequest();
    request.setContentId(UUID.randomUUID());
    request.setText("재미있어요");
    request.setRating(5.0);

    ReviewDto response =
        ReviewDto.builder()
            .id(UUID.randomUUID())
            .contentId(request.getContentId())
            .text(request.getText())
            .rating(request.getRating())
            .build();

    given(reviewService.create(any(ReviewCreateRequest.class), eq(userId))).willReturn(response);

    // when & then
    mockMvc
        .perform(
            post("/api/reviews")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(authentication))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.text").value("재미있어요"));
  }

  @Test
  @DisplayName("리뷰 단건 조회 성공")
  @WithMockUser
  void getReview_Success() throws Exception {
    // given
    UUID reviewId = UUID.randomUUID();
    ReviewDto response = ReviewDto.builder().id(reviewId).text("리뷰 내용").rating(4.5).build();

    given(reviewService.findById(eq(reviewId), any())).willReturn(response);

    // when & then
    mockMvc
        .perform(get("/api/reviews/{reviewId}", reviewId).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(reviewId.toString()))
        .andExpect(jsonPath("$.text").value("리뷰 내용"));
  }

  @Test
  @DisplayName("콘텐츠별 리뷰 목록 조회 성공")
  @WithMockUser
  void getReviews_Success() throws Exception {
    // given
    UUID contentId = UUID.randomUUID();
    List<ReviewDto> response =
        List.of(
            ReviewDto.builder().id(UUID.randomUUID()).text("리뷰1").build(),
            ReviewDto.builder().id(UUID.randomUUID()).text("리뷰2").build());

    given(reviewService.findByContentId(eq(contentId), any())).willReturn(response);

    // when & then
    mockMvc
        .perform(get("/api/reviews").param("contentId", contentId.toString()).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].text").value("리뷰1"));
  }
}
