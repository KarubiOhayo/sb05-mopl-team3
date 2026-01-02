package io.mopl.api.auth.service;

import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.core.error.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  /** 임시 비밀번호를 이메일로 전송 */
  @Retryable(
      retryFor = {MailException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2))
  public void sendTemporaryPassword(String toEmail, String temporaryPassword) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom("모두의 플리 <" + fromEmail + ">");
      helper.setTo(toEmail);
      helper.setSubject("[모두의 플리] 임시 비밀번호 발급 안내");
      helper.setText(buildEmailContent(temporaryPassword));

      mailSender.send(message);

    } catch (MessagingException | MailException e) {
      throw new BusinessException(AuthErrorCode.EMAIL_SEND_FAILED, e);
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
        ※ 본 메일은 발신전용으로 회신되지 않습니다.

        감사합니다.
        """,
        temporaryPassword);
  }
}
