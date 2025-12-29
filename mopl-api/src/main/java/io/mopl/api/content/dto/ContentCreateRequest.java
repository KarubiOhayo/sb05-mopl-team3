package io.mopl.api.content.dto;

import io.mopl.api.content.domain.ContentType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentCreateRequest {

  private ContentType type;
  private String title;
  private String description;
  private List<String> tags;
}
