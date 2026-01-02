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
