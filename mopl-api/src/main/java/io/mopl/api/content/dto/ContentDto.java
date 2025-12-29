package io.mopl.api.content.dto;

import io.mopl.api.content.domain.ContentType;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDto {

  private UUID id;
  private ContentType type;
  private String title;
  private String description;
  private String thumbnailUrl;
  private List<String> tags;
  private double averageRating;
  private int reviewCount;
  private long watcherCount;
}
