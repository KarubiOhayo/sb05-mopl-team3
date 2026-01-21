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

  @Mapping(target = "author", source = "author")
  @Mapping(target = "id", source = "review.id")
  @Mapping(target = "contentId", source = "review.contentId")
  @Mapping(target = "text", source = "review.text")
  @Mapping(target = "rating", source = "review.rating")
  ReviewDto toDto(Review review, UserSummary author);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "authorId", source = "authorId")
  Review toEntity(ReviewCreateRequest request, UUID authorId);
}
