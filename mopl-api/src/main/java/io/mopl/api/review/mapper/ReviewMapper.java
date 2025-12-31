package io.mopl.api.review.mapper;

import io.mopl.api.review.domain.Review;
import io.mopl.api.review.dto.ReviewCreateRequest;
import io.mopl.api.review.dto.ReviewDto;
import io.mopl.api.user.dto.UserSummary;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

  /**
   * Map a Review entity and its author summary to a ReviewDto.
   *
   * @param review the source Review entity
   * @param author the UserSummary to set as the review's author in the DTO
   * @return a ReviewDto containing the review's id, contentId, text, rating, and the provided author information
   */
  @Mapping(target = "author", source = "author")
  @Mapping(target = "id", source = "review.id")
  @Mapping(target = "contentId", source = "review.contentId")
  @Mapping(target = "text", source = "review.text")
  @Mapping(target = "rating", source = "review.rating")
  ReviewDto toDto(Review review, UserSummary author);

  /**
   * Create a Review entity from a creation request and an author's UUID.
   *
   * The resulting entity is populated with values from the request and has its authorId set
   * to the provided UUID. The entity's id, createdAt, and updatedAt fields are not set.
   *
   * @param request  the review creation request containing contentId, text, rating, and related fields
   * @param authorId the UUID of the review's author
   * @return a Review entity populated from the request with authorId set; id, createdAt, and updatedAt unset
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "authorId", source = "authorId")
  Review toEntity(ReviewCreateRequest request, UUID authorId);
}