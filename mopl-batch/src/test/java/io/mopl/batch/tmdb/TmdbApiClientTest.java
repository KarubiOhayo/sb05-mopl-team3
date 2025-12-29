package io.mopl.batch.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import io.mopl.batch.content.tmdb.TmdbApiClient;
import io.mopl.batch.content.tmdb.dto.TmdbMovieResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TmdbApiClientTest {

  @Autowired private TmdbApiClient tmdbApiClient;

  @Test
  @DisplayName("TMDB 인기 영화 목록 조회 테스트")
  void fetchPopularMovies() {
    // given
    int page = 1;

    // when
    List<TmdbMovieResponse> movies = tmdbApiClient.fetchPopularMovies(page);

    // then
    assertThat(movies).isNotEmpty();
    movies.forEach(
        movie -> {
          System.out.println("Title: " + movie.getTitle());
          System.out.println("Genre Ids: " + movie.getGenreIds());
          System.out.println("Overview: " + movie.getOverview());
          System.out.println("--------------------------------------------------");
        });
  }
}
