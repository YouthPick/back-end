package com.bop.youthpick.global.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리. 모든 컨트롤러 예외를 여기서 {@link ErrorResponse}로 변환한다. 컨트롤러는 try-catch로 에러 응답을 직접 만들지 않는다(여기로
 * 위임).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        BindingResult bindingResult = exception.getBindingResult();
        List<ErrorResponse.FieldErrorDetail> details =
                bindingResult.getFieldErrors().stream()
                        .map(
                                error ->
                                        new ErrorResponse.FieldErrorDetail(
                                                error.getField(),
                                                error.getRejectedValue() == null
                                                        ? ""
                                                        : error.getRejectedValue().toString(),
                                                error.getDefaultMessage()))
                        .toList();

        ErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception) {
        List<ErrorResponse.FieldErrorDetail> details =
                exception.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        new ErrorResponse.FieldErrorDetail(
                                                fieldName(violation),
                                                violation.getInvalidValue() == null
                                                        ? ""
                                                        : violation.getInvalidValue().toString(),
                                                violation.getMessage()))
                        .toList();

        ErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, details));
    }

    // 우리가 정의하는 비즈니스 에러
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // 개발자가 예상치 못한 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.error("예상치 못한 서버 오류", exception);
        ErrorCode errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    private String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int separatorIndex = path.lastIndexOf('.');
        if (separatorIndex < 0 || separatorIndex == path.length() - 1) {
            return path;
        }
        return path.substring(separatorIndex + 1);
    }

    @RequiredArgsConstructor
    @Getter
    enum GlobalErrorCode implements ErrorCode {
        INTERNAL_SERVER_ERROR(
                HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부에 예기치 않은 오류가 발생했습니다."),
        INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력 형태가 올바르지 않습니다.");

        private final HttpStatus status;
        private final String code;
        private final String message;
    }
}
