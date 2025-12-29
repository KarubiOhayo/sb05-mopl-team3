package io.mopl.api.review.dto;

import io.mopl.api.user.dto.UserSummary;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {

  private UUID id;
  private UUID contentId;
  private UserSummary author;
  private String text;
  private double rating;
}
