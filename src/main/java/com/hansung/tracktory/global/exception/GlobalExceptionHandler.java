package com.hansung.tracktory.global.exception;

import com.hansung.tracktory.global.response.ApiResponse;
import com.hansung.tracktory.global.response.ErrorPayload;
import com.hansung.tracktory.global.response.FieldError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Tier 1: 도메인 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.name(), e.getMessage());
        ErrorPayload error = ErrorPayload.of(errorCode.name(), e.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.fail(error));
    }

    // Tier 2: 요청 형식 검증 실패 (@Valid 위반) — 필드별 details 매핑
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        List<FieldError> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("ValidationFailed: {}", details);
        ErrorPayload error = ErrorPayload.of(errorCode.name(), errorCode.getDefaultMessage(), details);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.fail(error));
    }

    // Tier 3: 미처리 예외 safety net
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("UnhandledException", e);
        ErrorPayload error = ErrorPayload.of(errorCode.name(), errorCode.getDefaultMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.fail(error));
    }
}
