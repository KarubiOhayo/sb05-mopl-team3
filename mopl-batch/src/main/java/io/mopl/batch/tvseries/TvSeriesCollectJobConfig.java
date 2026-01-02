package io.mopl.batch.tvseries;

import io.mopl.batch.client.tmdb.dto.TmdbTvSeriesResponse;
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
public class TvSeriesCollectJobConfig {

  private final TmdbTvSeriesItemReader reader;
  private final TmdbTvSeriesItemProcessor processor;
  private final ContentWithTagWriter writer;

  @Bean
  public Job tvSeriesCollectJob(JobRepository jobRepository, Step tvSeriesCollectStep) {
    return new JobBuilder("tvSeriesCollectJob", jobRepository).start(tvSeriesCollectStep).build();
  }

  @Bean
  public Step tvSeriesCollectStep(JobRepository jobRepository) {
    return new StepBuilder("tvSeriesCollectStep", jobRepository)
        .<TmdbTvSeriesResponse, Content>chunk(10)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
  }
}
