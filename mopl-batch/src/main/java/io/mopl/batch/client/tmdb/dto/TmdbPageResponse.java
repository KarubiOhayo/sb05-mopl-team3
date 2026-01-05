package io.mopl.batch.client.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** TMDB 페이지네이션 응답 DTO. */
@Getter
@NoArgsConstructor
@ToString
public class TmdbPageResponse<T> {
  @JsonProperty("page")
  private int page;

  @JsonProperty("results")
  private List<T> results;

  @JsonProperty("total_pages")
  private int totalPages;

  @JsonProperty("total_results")
  private int totalResults;
}
