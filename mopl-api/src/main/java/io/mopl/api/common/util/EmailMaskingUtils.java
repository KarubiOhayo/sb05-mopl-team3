package io.mopl.api.common.util;

/** 이메일 마스킹 유틸리티 */
public class EmailMaskingUtils {

  private EmailMaskingUtils() {}

  /** 이메일 주소를 마스킹 처리 */
  public static String maskEmail(String email) {
    if (email == null || email.isEmpty()) {
      return "";
    }

    // @ 기준으로 분리
    int atIndex = email.indexOf('@');
    if (atIndex <= 0) {
      // @ 없거나 맨 앞에 있으면 전체 마스킹
      return "***";
    }

    String localPart = email.substring(0, atIndex);
    String domain = email.substring(atIndex);

    // 로컬 파트 마스킹
    String maskedLocal;
    if (localPart.length() <= 3) {
      // 3자 이하면 첫 글자만 보이고 나머지 ***
      maskedLocal = localPart.charAt(0) + "***";
    } else {
      // 4자 이상이면 앞 3자만 보이고 나머지 ***
      maskedLocal = localPart.substring(0, 3) + "***";
    }

    return maskedLocal + domain;
  }
}
