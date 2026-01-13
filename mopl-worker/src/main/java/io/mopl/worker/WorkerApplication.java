package io.mopl.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 워커 모듈의 스프링 부트 엔트리 포인트.
 *
 * <p>설정 프로퍼티 스캔을 활성화한다.
 */
@EnableRetry
@SpringBootApplication
@ConfigurationPropertiesScan
public class WorkerApplication {
  /** 애플리케이션을 부트스트랩한다. */
  public static void main(String[] args) {
    SpringApplication.run(WorkerApplication.class, args);
  }
}
