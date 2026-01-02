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
public class MovieCollectScheduler {

  private final JobOperator jobOperator;
  private final Job movieCollectJob;

  // 매일 새벽 4시 실행 (초 분 시 일 월 요일)
  @Scheduled(cron = "${batch.schedule.movie-collect-cron}")
  public void runMovieCollectJob() {
    try {
      log.info("Movie 수집 배치 작업 시작: {}", System.currentTimeMillis());

      JobParameters jobParameters =
          new JobParametersBuilder()
              .addLong(
                  "requestTime", System.currentTimeMillis()) // 매번 새로운 JobInstance 생성을 위해 시간 파라미터 추가
              .toJobParameters();

      jobOperator.start(movieCollectJob, jobParameters);

      log.info("Movie 수집 배치 작업 종료");
    } catch (Exception e) {
      log.error("Movie 수집 배치 작업 실패", e);
    }
  }
}
