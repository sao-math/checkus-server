package saomath.checkusserver.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import saomath.checkusserver.auth.dto.ResponseBase;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * HTTP 메시지 읽기 실패 예외 처리 (잘못된 JSON 형식 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseBase<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        log.warn("HTTP message not readable exception: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ResponseBase.error("잘못된 요청 형식입니다. JSON 형식을 확인해 주세요."));
    }

    /**
     * 메서드 인자 타입 불일치 예외 처리 (날짜 형식 오류 등)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseBase<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String parameterName = ex.getName();
        String parameterType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("파라미터 '%s'의 값이 올바르지 않습니다. %s 형식이어야 합니다.", 
                parameterName, parameterType);
        
        log.warn("Method argument type mismatch: parameter={}, requiredType={}, value={}", 
                parameterName, parameterType, ex.getValue());
        
        return ResponseEntity.badRequest()
                .body(ResponseBase.error(message));
    }

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseBase<Object>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ResponseBase.error(ex.getMessage()));
    }

    /**
     * 인증 예외 처리
     */
    @ExceptionHandler(saomath.checkusserver.common.exception.AuthenticationException.class)
    public ResponseEntity<ResponseBase<Object>> handleAuthenticationException(
            saomath.checkusserver.common.exception.AuthenticationException ex, WebRequest request) {
        
        log.warn("Authentication exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseBase.error(ex.getMessage()));
    }

    /**
     * Spring Security 인증 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseBase<Object>> handleSpringAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        log.warn("Spring Security authentication exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseBase.error("인증에 실패했습니다."));
    }

    /**
     * 접근 권한 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseBase<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseBase.error("접근 권한이 없습니다."));
    }

    /**
     * 중복 리소스 예외 처리
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ResponseBase<Object>> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        
        log.warn("Duplicate resource exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseBase.error(ex.getMessage()));
    }

    /**
     * 무효한 토큰 예외 처리
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ResponseBase<Object>> handleInvalidTokenException(
            InvalidTokenException ex, WebRequest request) {
        
        log.warn("Invalid token exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseBase.error(ex.getMessage()));
    }

    /**
     * 리소스 없음 예외 처리
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseBase<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        log.warn("Resource not found exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseBase.error(ex.getMessage()));
    }

    /**
     * 검증 실패 예외 처리 (@Valid 어노테이션)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseBase<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ResponseBase.error("입력값 검증에 실패했습니다.", errors));
    }

    /**
     * 바인딩 예외 처리
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResponseBase<Map<String, String>>> handleBindException(
            BindException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Binding failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ResponseBase.error("입력값 바인딩에 실패했습니다.", errors));
    }

    /**
     * 잘못된 인자 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseBase<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("Illegal argument exception: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ResponseBase.error("잘못된 요청 파라미터입니다: " + ex.getMessage()));
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseBase<Object>> handleGeneralException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseBase.error("예상치 못한 오류가 발생했습니다."));
    }

    /**
     * 일반적인 런타임 예외 처리 (가장 마지막에 위치)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseBase<Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        log.error("Runtime exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseBase.error("서버 내부 오류가 발생했습니다."));
    }
}
