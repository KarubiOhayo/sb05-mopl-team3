package io.mopl.api.content.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentUpdateRequest {

  @NotBlank(message = "타이틀은 필수입니다.")
  private String title;

  @NotBlank(message = "설명은 필수입니다.")
  private String description;
  private List<String> tags;
}
