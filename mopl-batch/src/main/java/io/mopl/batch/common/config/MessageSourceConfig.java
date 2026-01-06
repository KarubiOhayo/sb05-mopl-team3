package io.mopl.batch.common.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/** 배치/코어 메시지 번들을 로드하는 MessageSource 설정. */
@Configuration
public class MessageSourceConfig {

  /**
   * 다국어 메시지 조회를 위한 MessageSource 빈.
   *
   * @return 메시지 번들 설정이 적용된 MessageSource
   */
  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();

    messageSource.setBasenames("classpath:batch-messages", "classpath:core-messages");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setFallbackToSystemLocale(false);

    return messageSource;
  }
}
