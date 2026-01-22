package io.mopl.socket.common.error;

import io.mopl.core.error.BusinessException;
import io.mopl.core.error.ErrorResponse;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalMessageExceptionHandler {

  private final SimpMessagingTemplate messagingTemplate;

  @MessageExceptionHandler(MethodArgumentNotValidException.class)
  public void handleValidationException(MethodArgumentNotValidException ex, Principal principal) {
    log.warn("유효성 검증 실패: {}", principal != null ? principal.getName() : "unknown", ex);

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    sendError(principal, "MethodArgumentNotValidException", "입력값이 올바르지 않습니다.", errors);
  }

  @MessageExceptionHandler(BusinessException.class)
  public void handleBusinessException(BusinessException ex, Principal principal) {
    log.warn("커스텀 예외 발생: {}", principal != null ? principal.getName() : "unknown", ex);
    sendError(principal, ex.getClass().getSimpleName(), ex.getMessage(), null);
  }

  @MessageExceptionHandler(Exception.class)
  public void handleException(Exception ex, Principal principal) {
    log.error("알 수 없는 오류 발생: {}", principal != null ? principal.getName() : "unknown", ex);
    sendError(principal, "InternalServerError", "알 수 없는 오류가 발생했습니다.", null);
  }

  private void sendError(
      Principal principal, String exceptionName, String message, Map<String, String> details) {
    if (principal == null) {
      return;
    }

    ErrorResponse response =
        ErrorResponse.builder()
            .exceptionName(exceptionName)
            .message(message)
            .details(details)
            .build();

    messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", response);
  }
}
