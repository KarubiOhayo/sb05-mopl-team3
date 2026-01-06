package io.mopl.batch.tvseries;

import io.mopl.batch.client.tmdb.TmdbApiClient;
import io.mopl.batch.client.tmdb.dto.TmdbTvSeriesResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TMDB 인기 TV 시리즈 목록을 페이지 단위로 읽어오는 ItemReader.
 *
 * <p>내부 버퍼가 비면 다음 페이지를 호출해 채우고, 더 이상 없으면 null을 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@StepScope
public class TmdbTvSeriesItemReader implements ItemReader<TmdbTvSeriesResponse> {

  private final TmdbApiClient tmdbApiClient;

  private int currentPage = 1;
  private final Queue<TmdbTvSeriesResponse> buffer = new LinkedList<>();

  @Value("${tmdb.max-pages.tv-series:10}")
  private int maxPages;

  /**
   * 다음 TV 시리즈 항목을 반환한다.
   *
   * @return 다음 TV 시리즈 항목, 더 이상 없으면 null
   */
  @Override
  public @Nullable TmdbTvSeriesResponse read() {
    if (!buffer.isEmpty()) {
      return buffer.poll();
    }

    if (currentPage > maxPages) {
      return null;
    }

    log.info("TMDB API 호출: page={}", currentPage);
    List<TmdbTvSeriesResponse> tvSeries = tmdbApiClient.fetchPopularTvSeries(currentPage);

    if (tvSeries == null || tvSeries.isEmpty()) {
      return null;
    }

    buffer.addAll(tvSeries);
    currentPage++;

    return buffer.poll();
  }
}
