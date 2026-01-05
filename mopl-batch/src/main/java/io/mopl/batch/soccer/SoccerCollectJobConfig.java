package io.mopl.batch.soccer;

import io.mopl.batch.client.tsdb.dto.TsdbSoccerResponse;
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
public class SoccerCollectJobConfig {

  private final TsdbSoccerItemReader reader;
  private final TsdbSoccerItemProcessor processor;
  private final ContentWithTagWriter writer;

  @Bean
  public Job soccerCollectJob(JobRepository jobRepository, Step soccerCollectStep) {
    return new JobBuilder("soccerCollectJob", jobRepository).start(soccerCollectStep).build();
  }

  @Bean
  public Step soccerCollectStep(JobRepository jobRepository) {
    return new StepBuilder("soccerCollectStep", jobRepository)
        .<TsdbSoccerResponse, Content>chunk(10)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
  }
}
