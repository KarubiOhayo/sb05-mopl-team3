package io.mopl.api.auth.service;

import io.mopl.api.auth.oauth2.CustomOAuth2User;
import io.mopl.api.auth.oauth2.GoogleOAuth2UserInfo;
import io.mopl.api.auth.oauth2.KakaoOAuth2UserInfo;
import io.mopl.api.auth.oauth2.OAuth2UserInfo;
import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserLinkedProvider;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.service.UserLinkedProviderService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenService refreshTokenService;
  private final UserLinkedProviderService linkedProviderService;

  /** OAuth2 사용자 정보 로드 및 처리 */
  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    try {
      OAuth2User oAuth2User = super.loadUser(userRequest);
      String registrationId = userRequest.getClientRegistration().getRegistrationId();

      OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(userRequest, oAuth2User);

      // ✅ state에서 mode 확인
      String state = getStateFromRequest();
      boolean isLinkMode = state != null && state.contains(":mode=link");

      User user;
      if (isLinkMode) {
        // 연동 모드: 기존 사용자 조회만 수행 (신규 등록하지 않음)
        log.debug("OAuth2 연동 모드 - 기존 사용자 조회만 수행");
        user = findExistingUserForLinking(userRequest, oAuth2UserInfo);
      } else {
        // 로그인 모드: 기존 로직대로 처리
        log.debug("OAuth2 로그인 모드 - 일반 처리");
        user = processOAuth2User(userRequest, oAuth2UserInfo);
        forceLogout(user);
      }

      AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());
      return new CustomOAuth2User(user, oAuth2User.getAttributes(), authProvider);

    } catch (OAuth2AuthenticationException e) {
      throw e;
    } catch (Exception e) {
      log.error("OAuth2 유저 로드에 실패", e);
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  /** Request에서 state 파라미터 추출 */
  private String getStateFromRequest() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        return attributes.getRequest().getParameter("state");
      }
    } catch (Exception e) {
      log.warn("state 파라미터 추출 실패", e);
    }
    return null;
  }

  /** 연동 모드: 기존 사용자만 조회 (신규 등록하지 않음) */
  private User findExistingUserForLinking(
      OAuth2UserRequest userRequest, OAuth2UserInfo oAuth2UserInfo) {
    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());
    String providerId = oAuth2UserInfo.getProviderId();

    // 이미 해당 제공자로 등록된 사용자가 있는지 확인
    Optional<User> existingUser =
        userRepository.findByAuthProviderAndProviderUserId(authProvider, providerId);

    if (existingUser.isPresent()) {
      User user = existingUser.get();
      if (user.isLocked()) {
        throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
      }
      return user;
    }

    // 연동 모드에서는 신규 사용자를 생성하지 않고 임시 사용자 객체 반환
    // 실제 연동은 OAuth2AuthenticationSuccessHandler에서 처리됨
    log.debug("연동 모드 - 신규 OAuth2 사용자 정보 (연동 대기 중)");

    // 임시 User 객체 생성 (DB에 저장하지 않음)
    String temporaryPassword = passwordEncoder.encode("OAUTH2_TEMP");
    return User.createOAuth2User(
        oAuth2UserInfo.getEmail(),
        oAuth2UserInfo.getName(),
        temporaryPassword,
        authProvider,
        providerId,
        oAuth2UserInfo.getProfileImageUrl());
  }

  /** OAuth2 제공자별 사용자 정보 객체 생성 */
  private OAuth2UserInfo getOAuth2UserInfo(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
    String registrationId = userRequest.getClientRegistration().getRegistrationId();

    return switch (registrationId.toLowerCase()) {
      case "google" -> new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
      case "kakao" -> new KakaoOAuth2UserInfo(oAuth2User.getAttributes());
      default -> {
        throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR)
            .addDetail("provider", registrationId)
            .addDetail("reason", "지원하지 않는 OAuth2 제공자");
      }
    };
  }

  /** OAuth2 사용자 가입 및 로그인 */
  private User processOAuth2User(OAuth2UserRequest userRequest, OAuth2UserInfo oAuth2UserInfo) {
    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    AuthProvider authProvider = AuthProvider.valueOf(registrationId.toUpperCase());
    String providerId = oAuth2UserInfo.getProviderId();

    // 1. User 테이블에서 해당 제공자로 등록된 사용자 찾기
    Optional<User> existingUserByProvider =
        userRepository.findByAuthProviderAndProviderUserId(authProvider, providerId);

    if (existingUserByProvider.isPresent()) {
      User user = existingUserByProvider.get();

      if (user.isLocked()) {
        throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
      }
      return user;
    }

    // 2. UserLinkedProvider 테이블에서 연동된 계정 찾기
    UserLinkedProvider linkedProvider =
        linkedProviderService.findByProviderAndProviderUserId(authProvider, providerId);

    if (linkedProvider != null) {
      log.debug("연동된 계정 발견: userId={}, provider={}", linkedProvider.getUserId(), authProvider);

      User user =
          userRepository
              .findById(linkedProvider.getUserId())
              .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

      if (user.isLocked()) {
        throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
      }

      return user;
    }

    // 3. 이메일로 기존 사용자 찾기
    String email = oAuth2UserInfo.getEmail();
    Optional<User> existingUserByEmail = userRepository.findByEmail(email);

    if (existingUserByEmail.isPresent()) {
      User existingUser = existingUserByEmail.get();
      String existingProviderName = existingUser.getAuthProvider().getDisplayName();

      throw new BusinessException(AuthErrorCode.OAUTH2_EMAIL_ALREADY_REGISTERED)
          .addDetail("existingProvider", existingProviderName);
    }

    // 4. 신규 사용자 등록
    return registerNewUser(authProvider, providerId, oAuth2UserInfo);
  }

  /** 신규 OAuth2 사용자 등록 */
  private User registerNewUser(
      AuthProvider authProvider, String providerId, OAuth2UserInfo oAuth2UserInfo) {
    String temporaryPassword = passwordEncoder.encode("OAUTH2_USER_NO_PASSWORD");

    User newUser =
        User.createOAuth2User(
            oAuth2UserInfo.getEmail(),
            oAuth2UserInfo.getName(),
            temporaryPassword,
            authProvider,
            providerId,
            oAuth2UserInfo.getProfileImageUrl());

    return userRepository.save(newUser);
  }

  /** 강제 로그아웃 */
  private void forceLogout(User user) {
    refreshTokenService.deleteRefreshToken(user.getId());
  }
}
