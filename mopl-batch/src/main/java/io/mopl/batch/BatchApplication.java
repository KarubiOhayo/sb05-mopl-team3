package io.mopl.batch;

import io.mopl.redis.config.RedisConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 배치 모듈의 스프링 부트 엔트리 포인트.
 *
 * <p>스케줄러, JPA 감사(Auditing), 설정 프로퍼티 스캔을 함께 활성화한다.
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
@ConfigurationPropertiesScan
@Import(RedisConfig.class)
public class BatchApplication {
  /** 애플리케이션을 부트스트랩한다. */
  public static void main(String[] args) {
    SpringApplication.run(BatchApplication.class, args);
  }
}
