package io.mopl.api.auth.service;

import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.core.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  /** 임시 비밀번호를 이메일로 전송 */
  public void sendTemporaryPassword(String toEmail, String temporaryPassword) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromEmail);
      message.setTo(toEmail);
      message.setSubject("[모두의 플리] 임시 비밀번호 발급 안내");
      message.setText(buildEmailContent(temporaryPassword));

      mailSender.send(message);
    } catch (Exception e) {
      throw new BusinessException(AuthErrorCode.EMAIL_SEND_FAILED);
    }
  }

  /** 이메일 본문 내용 생성 */
  private String buildEmailContent(String temporaryPassword) {
    return String.format(
        """
        안녕하세요, 모두의 플리입니다.
        임시 비밀번호가 발급되었습니다.

        임시 비밀번호: %s

        ※ 발급된 임시 비밀번호는 발급 시점부터 3분간 유효합니다.
        ※ 로그인 후 반드시 비밀번호를 변경해주세요.

        감사합니다.
        """,
        temporaryPassword);
  }
}
