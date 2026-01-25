package io.mopl.worker.content.index.dto;

import io.mopl.worker.content.index.domain.ContentType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentIndexRow {
  private UUID id;
  private ContentType type;
  private String title;
  private String description;
  private String thumbnailImageKey;
  private List<String> tags;
  private double averageRating;
  private int reviewCount;
  private long watcherCount;
  private Instant createdAt;
}
