package io.mopl.batch.common.error;

import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.core.error.ErrorCode;
import io.mopl.core.error.ErrorResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 배치 API에서 발생하는 예외를 공통 응답 포맷으로 변환한다.
 *
 * <p>BusinessException은 코드/메시지를 매핑하고, 그 외 예외는 500으로 처리한다.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class BatchExceptionHandler {

  private final MessageSource messageSource;

  /**
   * 비즈니스 예외를 처리한다.
   *
   * @param ex 비즈니스 예외
   * @return 표준 에러 응답
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    String resolvedMessage = resolveMessage(errorCode.getMessageKey());
    return buildResponse(
        errorCode, errorCode.getClass().getSimpleName(), resolvedMessage, ex.getDetails());
  }

  /**
   * 처리되지 않은 예외를 내부 서버 오류로 변환한다.
   *
   * @param ex 예외
   * @return 표준 에러 응답
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    log.error("배치 처리 중 예상치 못한 예외가 발생했습니다: ", ex);
    String resolvedMessage = resolveMessage(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessageKey());
    return buildResponse(
        CommonErrorCode.INTERNAL_SERVER_ERROR,
        ex.getClass().getSimpleName(),
        resolvedMessage,
        null);
  }

  private ResponseEntity<ErrorResponse> buildResponse(
      ErrorCode errorCode, String exceptionName, String message, Map<String, String> details) {
    ErrorResponse response =
        ErrorResponse.builder()
            .exceptionName(exceptionName)
            .message(message)
            .details(details == null || details.isEmpty() ? null : details)
            .build();

    return ResponseEntity.status(HttpStatusCode.valueOf(errorCode.getStatus())).body(response);
  }

  private String resolveMessage(String messageKey) {
    try {
      return messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
    } catch (Exception e) {
      return messageKey;
    }
  }
}
