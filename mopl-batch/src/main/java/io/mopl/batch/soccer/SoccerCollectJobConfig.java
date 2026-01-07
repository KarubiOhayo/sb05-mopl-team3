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

/** 축구 경기 수집 배치 잡 구성. */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SoccerCollectJobConfig {

  private final TsdbSoccerItemReader reader;
  private final TsdbSoccerItemProcessor processor;
  private final ContentWithTagWriter writer;

  /**
   * 축구 경기 수집 잡을 생성한다.
   *
   * @param jobRepository 잡 저장소
   * @param soccerCollectStep 실행 스텝
   * @return Job 인스턴스
   */
  @Bean
  public Job soccerCollectJob(JobRepository jobRepository, Step soccerCollectStep) {
    return new JobBuilder("soccerCollectJob", jobRepository).start(soccerCollectStep).build();
  }

  /**
   * TheSportsDB 데이터를 읽어 콘텐츠로 저장하는 스텝.
   *
   * @param jobRepository 잡 저장소
   * @return Step 인스턴스
   */
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
