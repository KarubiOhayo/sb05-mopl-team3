package io.mopl.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class JobLauncherController {

  private final JobOperator jobOperator;
  private final Job movieCollectJob;

  @PostMapping("/movies")
  public String runMovieCollectJob() {
    try {
      JobParameters jobParameters =
          new JobParametersBuilder()
              .addLong("requestTime", System.currentTimeMillis())
              .toJobParameters();

      jobOperator.start(movieCollectJob, jobParameters);

      return "Movie Collect Job Started!";
    } catch (Exception e) {
      log.error("Failed to launch job", e);
      return "Failed: " + e.getMessage();
    }
  }
}
