package io.mopl.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentCollectScheduler {

  private final JobOperator jobOperator;
  private final Job movieCollectJob;
  private final Job tvSeriesCollectJob;

  @Scheduled(cron = "${batch.schedule.movie-collect-cron}")
  public void runMovieCollectJob() {
    try {
      log.info("Movie 수집 배치 작업 시작: {}", System.currentTimeMillis());

      JobParameters jobParameters =
          new JobParametersBuilder()
              .addLong("requestTime", System.currentTimeMillis())
              .toJobParameters();

      jobOperator.start(movieCollectJob, jobParameters);

      log.info("Movie 수집 배치 작업 트리거 완료");
    } catch (Exception e) {
      log.error("Movie 수집 배치 작업 실패", e);
    }
  }

  @Scheduled(cron = "${batch.schedule.tv-series-collect-cron}")
  public void runTvSeriesCollectJob() {
    try {
      log.info("TvSeries 수집 배치 작업 시작: {}", System.currentTimeMillis());

      JobParameters jobParameters =
          new JobParametersBuilder()
              .addLong("requestTime", System.currentTimeMillis())
              .toJobParameters();

      jobOperator.start(tvSeriesCollectJob, jobParameters);

      log.info("TvSeries 수집 배치 작업 트리거 완료");
    } catch (Exception e) {
      log.error("TvSeries 수집 배치 작업 실패", e);
    }
  }
}
