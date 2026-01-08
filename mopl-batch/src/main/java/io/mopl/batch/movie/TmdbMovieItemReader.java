package io.mopl.batch.movie;

import io.mopl.batch.client.tmdb.TmdbApiClient;
import io.mopl.batch.client.tmdb.dto.TmdbMovieResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TMDB 인기 영화 목록을 페이지 단위로 읽어오는 ItemReader.
 *
 * <p>내부 버퍼가 비면 다음 페이지를 호출해 채우고, 버퍼가 소진되면 null을 반환한다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@StepScope
public class TmdbMovieItemReader implements ItemReader<TmdbMovieResponse> {

  private final TmdbApiClient tmdbApiClient;

  private int currentPage = 1;
  private final Queue<TmdbMovieResponse> buffer = new LinkedList<>();

  @Value("${tmdb.max-pages.movie:10}")
  private int maxPages;

  /**
   * 다음 영화 항목을 반환한다.
   *
   * @return 다음 영화 항목, 더 이상 없으면 null
   */
  @Override
  public TmdbMovieResponse read() {
    // 1. 버퍼에 데이터가 남아있으면 즉시 반환
    if (!buffer.isEmpty()) {
      return buffer.poll();
    }

    // 2. 버퍼가 비었고, 최대 페이지에 도달했다면 종료 (null 반환)
    if (currentPage > maxPages) {
      return null;
    }

    // 3. API 호출하여 데이터 채우기
    log.info("TMDB API 호출: page={}", currentPage);
    List<TmdbMovieResponse> movies = tmdbApiClient.fetchPopularMovies(currentPage);

    // 4. 결과가 없으면 종료
    if (movies == null || movies.isEmpty()) {
      return null;
    }

    // 5. 버퍼에 담고 페이지 증가
    buffer.addAll(movies);
    currentPage++;

    // 6. 방금 채운 버퍼에서 하나 꺼내서 반환
    return buffer.poll();
  }
}
