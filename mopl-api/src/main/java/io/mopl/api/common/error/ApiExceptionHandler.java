package io.mopl.api.common.error;

import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.core.error.ErrorCode;
import io.mopl.core.error.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

  private final MessageSource messageSource;

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
    ErrorCode errorCode = ex.getErrorCode();
    String resolvedMessage = resolveMessage(errorCode.getMessageKey());
    return buildResponse(
        errorCode, errorCode.getClass().getSimpleName(), resolvedMessage, ex.getDetails());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    Map<String, String> details = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
    ex.getBindingResult()
        .getGlobalErrors()
        .forEach(error -> details.put(error.getObjectName(), error.getDefaultMessage()));

    String resolvedMessage = resolveMessage(CommonErrorCode.INVALID_REQUEST.getMessageKey());
    return buildResponse(
        CommonErrorCode.INVALID_REQUEST,
        CommonErrorCode.INVALID_REQUEST.name(),
        resolvedMessage,
        details);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    Map<String, String> details = new HashMap<>();
    ex.getConstraintViolations()
        .forEach(
            violation ->
                details.put(violation.getPropertyPath().toString(), violation.getMessage()));

    String resolvedMessage = resolveMessage(CommonErrorCode.INVALID_REQUEST.getMessageKey());
    return buildResponse(
        CommonErrorCode.INVALID_REQUEST,
        CommonErrorCode.INVALID_REQUEST.name(),
        resolvedMessage,
        details);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception ex) {
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
