package io.mopl.batch.client.tsdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class TsdbSoccerResponse {

  @JsonProperty("idEvent")
  private Long id;

  @JsonProperty("strEvent")
  private String title;

  @JsonProperty("strFilename")
  private String description;

  @JsonProperty("strLeague")
  private String league;

  @JsonProperty("strVenue")
  private String venue;

  @JsonProperty("strThumb")
  private String thumbnailUrl;
}
