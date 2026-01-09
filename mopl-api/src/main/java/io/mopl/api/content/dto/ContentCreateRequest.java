package io.mopl.api.content.dto;

import io.mopl.api.content.domain.ContentType;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentCreateRequest {

  @NotNull(message = "타입은 필수입니다.")
  private ContentType type;

  @NotBlank(message = "타이틀은 필수입니다.")
  private String title;

  @NotBlank(message = "설명은 필수입니다.")
  private String description;

  private List<String> tags;
}
