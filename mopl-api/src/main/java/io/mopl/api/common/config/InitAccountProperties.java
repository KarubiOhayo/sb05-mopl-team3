package io.mopl.api.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "mopl.init")
public class InitAccountProperties {

  private Admin admin = new Admin();
  private String testUserPassword;

  @Getter
  @Setter
  public static class Admin {
    private String email;
    private String password;
    private String name;
  }
}
