package io.mopl.socket;

import io.mopl.redis.config.RedisConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(RedisConfig.class)
@SpringBootApplication
public class SocketApplication {
  public static void main(String[] args) {
    SpringApplication.run(SocketApplication.class, args);
  }
}
