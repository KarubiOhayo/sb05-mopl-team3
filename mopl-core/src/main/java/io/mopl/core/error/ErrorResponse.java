package io.mopl.core.error;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

  private String exceptionName;
  private String message;
  private Map<String, String> details;
}
