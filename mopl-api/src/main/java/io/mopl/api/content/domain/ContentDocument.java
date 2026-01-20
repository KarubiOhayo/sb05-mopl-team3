package io.mopl.api.content.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Document(indexName = "contents")
public class ContentDocument {
  @Id private String id;

  @Field(type = FieldType.Keyword)
  private UUID contentId;

  @Field(type = FieldType.Keyword)
  private ContentType type;

  @Field(type = FieldType.Text)
  private String title;

  @Field(type = FieldType.Text)
  private String description;

  @Field(type = FieldType.Keyword)
  private String thumbnailImageKey;

  @Field(type = FieldType.Keyword)
  private List<String> tags;

  @Field(type = FieldType.Double)
  private double averageRating;

  @Field(type = FieldType.Integer)
  private int reviewCount;

  @Field(type = FieldType.Long)
  private long watcherCount;

  @Field(type = FieldType.Date)
  private Instant createdAt;
}
