package io.mopl.batch.client.tmdb;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.mopl.batch.client.tmdb.dto.TmdbMovieResponse;
import io.mopl.batch.client.tmdb.dto.TmdbPageResponse;
import io.mopl.batch.client.tmdb.dto.TmdbTvSeriesResponse;
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

/** TMDB API 호출을 담당하는 클라이언트. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbApiClient {

  private final RestTemplate restTemplate;
  private final MeterRegistry meterRegistry;

  @Value("${tmdb.access-token}")
  private String accessToken;

  @Value("${tmdb.language}")
  private String language;

  private static final String BASE_URL = "https://api.themoviedb.org/3";

  /**
   * TMDB 인기 영화 목록을 페이지 단위로 조회한다.
   *
   * @param page 조회할 페이지
   * @return 영화 목록 (없으면 빈 리스트)
   * @throws BusinessException 호출 실패 시
   */
  public List<TmdbMovieResponse> fetchPopularMovies(int page) {
    String url = String.format("%s/movie/popular?language=%s&page=%d", BASE_URL, language, page);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.set("accept", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      ResponseEntity<TmdbPageResponse<TmdbMovieResponse>> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

      if (response.getBody() != null && response.getBody().getResults() != null) {
        return response.getBody().getResults();
      }
      return Collections.emptyList();
    } catch (Exception e) {
      Counter.builder("batch.external.api.errors")
          .tags("client", "tmdb", "operation", "popular_movies")
          .register(meterRegistry)
          .increment();
      log.error("TMDB API 호출 실패: {}", e.getMessage(), e);
      throw new BusinessException(BatchErrorCode.TMDB_API_CALL_ERROR)
          .addDetail("page", String.valueOf(page));
    } finally {
      sample.stop(
          Timer.builder("batch.external.api.duration")
              .tags("client", "tmdb", "operation", "popular_movies")
              .register(meterRegistry));
    }
  }

  /**
   * TMDB 인기 TV 시리즈 목록을 페이지 단위로 조회한다.
   *
   * @param page 조회할 페이지
   * @return TV 시리즈 목록 (없으면 빈 리스트)
   * @throws BusinessException 호출 실패 시
   */
  public List<TmdbTvSeriesResponse> fetchPopularTvSeries(int page) {
    String url = String.format("%s/tv/popular?language=%s&page=%d", BASE_URL, language, page);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + accessToken);
    headers.set("accept", "application/json");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      ResponseEntity<TmdbPageResponse<TmdbTvSeriesResponse>> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

      if (response.getBody() != null && response.getBody().getResults() != null) {
        return response.getBody().getResults();
      }
      return Collections.emptyList();
    } catch (Exception e) {
      Counter.builder("batch.external.api.errors")
          .tags("client", "tmdb", "operation", "popular_tv_series")
          .register(meterRegistry)
          .increment();
      log.error("TMDB API 호출 실패: {}", e.getMessage(), e);
      throw new BusinessException(BatchErrorCode.TMDB_API_CALL_ERROR)
          .addDetail("page", String.valueOf(page));
    } finally {
      sample.stop(
          Timer.builder("batch.external.api.duration")
              .tags("client", "tmdb", "operation", "popular_tv_series")
              .register(meterRegistry));
    }
  }
}
