package io.mopl.batch.content.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class TmdbMovieResponse {
  @JsonProperty("id")
  private Long id;

  @JsonProperty("title")
  private String title;

  @JsonProperty("overview")
  private String overview;

  @JsonProperty("poster_path")
  private String posterPath;

  @JsonProperty("genre_ids")
  private List<Integer> genreIds;
}
