package io.mopl.api.auth.jwt;

import com.nimbusds.jose.JOSEException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenGenerationTest {

  @Test
  void generateTestToken() throws JOSEException {
    // application-dev.yml에 설정된 시크릿 키
    String secret = "dev-secret-key-must-be-at-least-32-characters-long-for-hs256-algorithm";
    long validityInSeconds = 86400; // 24시간

    JwtTokenProvider provider = new JwtTokenProvider(secret, validityInSeconds);

    // 테스트용 사용자 정보 (DB에 존재하는 사용자 ID: 김철수)

    String email = "chulsu@naver.com";
    String role = "USER";
    UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    String token = provider.createAccessToken(userId, email, role);

    System.out.println("\n\n==================================================");
    System.out.println("Postman용 Access Token (Bearer Token):");
    System.out.println(token);
    System.out.println("==================================================\n\n");
  }
}
