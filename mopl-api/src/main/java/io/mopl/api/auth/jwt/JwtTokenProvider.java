package io.mopl.api.auth.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.access-token-validity-in-seconds}")
  private long accessTokenValidityInSeconds;

  @Value("${jwt.refresh-token-validity-in-seconds:604800}")
  private long refreshTokenValidityInSeconds;

  private MACSigner signer;
  private MACVerifier verifier;
  private long accessTokenValidityInMilliseconds;
  private long refreshTokenValidityInMilliseconds;

  @PostConstruct
  protected void init() {
    try {
      byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
      if (secretBytes.length < 32) {
        throw new IllegalArgumentException("JWT secret 키는 최소 32바이트(256비트) 이상이어야 합니다");
      }
      this.signer = new MACSigner(secretBytes);
      this.verifier = new MACVerifier(secretBytes);
      this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
      this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
    } catch (KeyLengthException e) {
      log.error("JWT secret 키 길이가 유효하지 않습니다: {}", e.getMessage());
      throw new IllegalArgumentException(
          "JWT secret 키는 HS256 알고리즘을 위해 최소 32바이트(256비트) 이상이어야 합니다", e);
    } catch (JOSEException e) {
      log.error("JWT signer/verifier 초기화 실패: {}", e.getMessage());
      throw new IllegalArgumentException("JWT 컴포넌트 초기화 실패", e);
    }
  }

  /** Access Token 생성 */
  public String createAccessToken(UUID userId, String email, String role) {
    try {
      Date now = new Date();
      Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

      JWTClaimsSet claimsSet =
          new JWTClaimsSet.Builder()
              .subject(userId.toString())
              .claim("email", email)
              .claim("role", role)
              .issueTime(now)
              .expirationTime(validity)
              .build();

      SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

      signedJWT.sign(signer);

      return signedJWT.serialize();
    } catch (JOSEException e) {
      log.error("JWT 토큰 생성 실패: {}", e.getMessage());
      throw new RuntimeException("JWT 토큰 생성에 실패했습니다", e);
    }
  }

  /** Refresh Token 생성 (JWT 기반) */
  public String createRefreshToken(UUID userId) {
    try {
      Date now = new Date();
      Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

      JWTClaimsSet claimsSet =
          new JWTClaimsSet.Builder()
              .subject(userId.toString())
              .claim("type", "refresh")
              .issueTime(now)
              .expirationTime(validity)
              .build();

      SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

      signedJWT.sign(signer);

      return signedJWT.serialize();
    } catch (JOSEException e) {
      log.error("리프레시 토큰 생성 실패: {}", e.getMessage());
      throw new RuntimeException("리프레시 토큰 생성에 실패했습니다", e);
    }
  }

  /** 토큰에서 userId 추출 */
  public UUID getUserId(String token) {
    JWTClaimsSet claims = parseClaims(token);
    return UUID.fromString(claims.getSubject());
  }

  /** 토큰에서 email 추출 */
  public String getEmail(String token) {
    try {
      JWTClaimsSet claims = parseClaims(token);
      return claims.getStringClaim("email");
    } catch (ParseException e) {
      log.error("JWT에서 email 추출 실패: {}", e.getMessage());
      throw new RuntimeException("JWT에서 email 추출에 실패했습니다", e);
    }
  }

  /** 토큰에서 role 추출 */
  public String getRole(String token) {
    try {
      JWTClaimsSet claims = parseClaims(token);
      return claims.getStringClaim("role");
    } catch (ParseException e) {
      log.error("JWT에서 role 추출 실패: {}", e.getMessage());
      throw new RuntimeException("JWT에서 role 추출에 실패했습니다", e);
    }
  }

  /** 토큰 유효성 검증 */
  public boolean validateToken(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);

      if (!signedJWT.verify(verifier)) {
        log.error("JWT 서명 검증 실패");
        return false;
      }

      Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
      if (expirationTime != null && expirationTime.before(new Date())) {
        log.error("JWT 토큰 만료");
        return false;
      }

      return true;
    } catch (Exception e) {
      log.error("Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }

  /** 토큰 파싱 */
  private JWTClaimsSet parseClaims(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      return signedJWT.getJWTClaimsSet();
    } catch (ParseException e) {
      log.error("JWT 파싱 실패: {}", e.getMessage());
      throw new RuntimeException("JWT 파싱에 실패했습니다", e);
    }
  }
}
