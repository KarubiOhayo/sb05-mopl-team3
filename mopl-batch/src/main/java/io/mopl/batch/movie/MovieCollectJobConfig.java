package io.mopl.batch.movie;

import io.mopl.batch.client.tmdb.dto.TmdbMovieResponse;
import io.mopl.batch.common.writer.ContentWithTagWriter;
import io.mopl.batch.content.domain.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 영화 수집 배치 잡 구성. */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MovieCollectJobConfig {

  private final TmdbMovieItemReader reader;
  private final TmdbMovieItemProcessor processor;
  private final ContentWithTagWriter writer;

  /**
   * 영화 수집 잡을 생성한다.
   *
   * @param jobRepository 잡 저장소
   * @param movieCollectStep 실행 스텝
   * @return Job 인스턴스
   */
  @Bean
  public Job movieCollectJob(JobRepository jobRepository, Step movieCollectStep) {
    return new JobBuilder("movieCollectJob", jobRepository).start(movieCollectStep).build();
  }

  /**
   * TMDB 영화 데이터를 읽어 콘텐츠로 저장하는 스텝.
   *
   * @param jobRepository 잡 저장소
   * @return Step 인스턴스
   */
  @Bean
  public Step movieCollectStep(JobRepository jobRepository) {
    return new StepBuilder("movieCollectStep", jobRepository)
        .<TmdbMovieResponse, Content>chunk(10)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
  }
}
