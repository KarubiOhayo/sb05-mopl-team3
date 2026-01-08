package io.mopl.socket.common.error;

import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.core.error.ErrorCode;
import io.mopl.core.error.ErrorResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class SocketExceptionHandler {

  private final MessageSource messageSource;

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    ErrorCode errorCode = e.getErrorCode();
    String message = messageSource.getMessage(errorCode.getMessageKey(), null, Locale.getDefault());

    log.error("[BusinessException] code: {}, message: {}", errorCode.getMessageKey(), message);

    ErrorResponse response =
        ErrorResponse.builder()
            .exceptionName(e.getClass().getSimpleName())
            .message(message)
            .details(e.getDetails())
            .build();

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("[Exception] message: {}", e.getMessage(), e);

    ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
    String message = messageSource.getMessage(errorCode.getMessageKey(), null, Locale.getDefault());

    ErrorResponse response =
        ErrorResponse.builder()
            .exceptionName(e.getClass().getSimpleName())
            .message(message)
            .build();

    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }
}
