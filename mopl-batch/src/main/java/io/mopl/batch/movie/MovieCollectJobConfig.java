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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MovieCollectJobConfig {

  private final TmdbMovieItemReader reader;
  private final TmdbMovieItemProcessor processor;
  private final ContentWithTagWriter writer;

  @Bean
  public Job movieCollectJob(JobRepository jobRepository, Step movieCollectStep) {
    return new JobBuilder("movieCollectJob", jobRepository).start(movieCollectStep).build();
  }

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
