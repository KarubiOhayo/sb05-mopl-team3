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

/** TV 시리즈 수집 배치 잡 구성. */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TvSeriesCollectJobConfig {

  private final TmdbTvSeriesItemReader reader;
  private final TmdbTvSeriesItemProcessor processor;
  private final ContentWithTagWriter writer;

  /**
   * TV 시리즈 수집 잡을 생성한다.
   *
   * @param jobRepository 잡 저장소
   * @param tvSeriesCollectStep 실행 스텝
   * @return Job 인스턴스
   */
  @Bean
  public Job tvSeriesCollectJob(JobRepository jobRepository, Step tvSeriesCollectStep) {
    return new JobBuilder("tvSeriesCollectJob", jobRepository).start(tvSeriesCollectStep).build();
  }

  /**
   * TMDB TV 시리즈 데이터를 읽어 콘텐츠로 저장하는 스텝.
   *
   * @param jobRepository 잡 저장소
   * @return Step 인스턴스
   */
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
