package io.mopl.batch.common.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** 외부 API 호출용 {@link RestTemplate} 빈을 구성한다. */
@Configuration
public class RestTemplateConfig {

  /**
   * 연결/읽기 타임아웃을 설정한 RestTemplate 빈.
   *
   * @return 설정이 적용된 RestTemplate
   */
  @Bean
  public RestTemplate restTemplate() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(10));
    return new RestTemplate(factory);
  }
}
