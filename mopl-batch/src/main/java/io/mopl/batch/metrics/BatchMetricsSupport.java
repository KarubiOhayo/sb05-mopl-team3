package io.mopl.batch.metrics;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;

public final class BatchMetricsSupport {

  private BatchMetricsSupport() {}

  public static String resolveJobName() {
    StepExecution stepExecution = resolveStepExecution();
    if (stepExecution == null) {
      return "unknown";
    }
    JobExecution jobExecution = stepExecution.getJobExecution();
    if (jobExecution == null || jobExecution.getJobInstance() == null) {
      return "unknown";
    }
    return jobExecution.getJobInstance().getJobName();
  }

  public static String resolveStepName() {
    StepExecution stepExecution = resolveStepExecution();
    return stepExecution == null ? "unknown" : stepExecution.getStepName();
  }

  public static String resolveJobParameter(String key) {
    StepExecution stepExecution = resolveStepExecution();
    if (stepExecution == null) {
      return null;
    }
    JobExecution jobExecution = stepExecution.getJobExecution();
    if (jobExecution == null) {
      return null;
    }
    JobParameters parameters = jobExecution.getJobParameters();
    if (parameters == null) {
      return null;
    }
    return parameters.getString(key);
  }

  private static StepExecution resolveStepExecution() {
    StepContext stepContext = StepSynchronizationManager.getContext();
    return stepContext == null ? null : stepContext.getStepExecution();
  }
}
