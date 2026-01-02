package io.mopl.batch.client.tmdb;

import io.mopl.batch.client.tmdb.dto.TmdbMovieResponse;
import io.mopl.batch.client.tmdb.dto.TmdbPageResponse;
import io.mopl.batch.common.BatchErrorCode;
import io.mopl.core.error.BusinessException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbApiClient {

  private final RestTemplate restTemplate;

  @Value("${tmdb.access-token}")
  private String accessToken;

  @Value("${tmdb.language}")
  private String language;

  private static final String BASE_URL = "https://api.themoviedb.org/3";

  public List<TmdbMovieResponse> fetchPopularMovies(int page) {
    String url = String.format("%s/movie/popular?language=%s&page=%d", BASE_URL, language, page);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.set("accept", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<TmdbPageResponse<TmdbMovieResponse>> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

      if (response.getBody() != null && response.getBody().getResults() != null) {
        return response.getBody().getResults();
      }
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("TMDB API 호출 실패: {}", e.getMessage(), e);
      throw new BusinessException(BatchErrorCode.TMDB_API_CALL_ERROR)
          .addDetail("page", String.valueOf(page));
    }
  }
}
