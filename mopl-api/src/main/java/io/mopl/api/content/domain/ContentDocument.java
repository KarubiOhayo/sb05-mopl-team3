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
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Document(indexName = "contents", createIndex = false)
public class ContentDocument {
  @Id private String id;

  @Field(type = FieldType.Keyword)
  private UUID contentId;

  @Field(type = FieldType.Keyword)
  private ContentType type;

  @MultiField(
      mainField = @Field(type = FieldType.Text),
      otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)})
  private String title;

  @MultiField(
      mainField = @Field(type = FieldType.Text),
      otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)})
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
