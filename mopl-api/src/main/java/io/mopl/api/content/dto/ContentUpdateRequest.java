package io.mopl.api.content.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentUpdateRequest {

  private String title;
  private String description;
  private List<String> tags;
}
