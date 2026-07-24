package com.bop.youthpick.global.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    // 필수 @RequestParam이 누락된 경우
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception) {
        ErrorResponse.FieldErrorDetail detail =
                new ErrorResponse.FieldErrorDetail(
                        exception.getParameterName(),
                        "",
                        exception.getParameterName() + "는 필수입니다.");

        ErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, List.of(detail)));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(
            MissingServletRequestPartException exception) {
        ErrorResponse.FieldErrorDetail detail =
                new ErrorResponse.FieldErrorDetail(
                        exception.getRequestPartName(), "", "파일은 필수입니다.");
        ErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, List.of(detail)));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded() {
        ErrorCode errorCode = GlobalErrorCode.FILE_SIZE_EXCEEDED;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // @RequestParam 값이 대상 타입(LocalDateTime 등)으로 변환되지 않는 경우
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        ErrorResponse.FieldErrorDetail detail =
                new ErrorResponse.FieldErrorDetail(
                        exception.getName(),
                        exception.getValue() == null ? "" : exception.getValue().toString(),
                        "유효하지 않은 형식입니다.");

        ErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, List.of(detail)));
    }

    // 요청 body의 JSON을 파싱하지 못한 경우 (필드 타입 불일치, 깨진 JSON 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception) {
        ErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // 우리가 정의하는 비즈니스 에러
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported() {
        ErrorCode errorCode = GlobalErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // 매핑되지 않은 경로(오타 URL, favicon.ico, 스캐너 봇 등). catch-all로 흘러가면 요청마다 500 + ERROR 로그가
    // application_logs에 적재되므로 여기서 404로 끊는다. 로깅하지 않는다.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound() {
        ErrorCode errorCode = GlobalErrorCode.RESOURCE_NOT_FOUND;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // Content-Type이 엔드포인트가 소비하는 타입과 다른 경우 (예: JSON 엔드포인트에 text/plain 전송)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported() {
        ErrorCode errorCode = GlobalErrorCode.UNSUPPORTED_MEDIA_TYPE;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // Accept 헤더로 협상 가능한 표현이 없는 경우. 클라이언트가 JSON을 받지 못하는 상황이므로
    // body를 쓰지 않고 상태 코드만 내린다(body를 쓰면 같은 예외가 재발한다).
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleMediaTypeNotAcceptable() {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    // 필수 헤더/쿠키 누락 등 요청 바인딩 실패
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ErrorResponse> handleServletRequestBinding() {
        ErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // Pageable의 ?sort=존재하지않는속성 등 Spring Data가 속성을 해석하지 못한 경우.
    // 사용자 입력 오류이므로 500(S001)이 아니라 C001로 내린다.
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(
            PropertyReferenceException exception) {
        ErrorResponse.FieldErrorDetail detail =
                new ErrorResponse.FieldErrorDetail(
                        "sort", exception.getPropertyName(), "정렬할 수 없는 속성입니다.");
        ErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, List.of(detail)));
    }

    // 컨트롤러에 @Validated 없이 파라미터 제약만 있을 때 Spring 6.1+ 내장 검증이 던지는 예외.
    // ConstraintViolationException 경로와 동일하게 C001로 수렴시킨다.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException exception) {
        List<ErrorResponse.FieldErrorDetail> details =
                exception.getParameterValidationResults().stream()
                        .flatMap(
                                result ->
                                        result.getResolvableErrors().stream()
                                                .map(
                                                        error ->
                                                                new ErrorResponse.FieldErrorDetail(
                                                                        result.getMethodParameter()
                                                                                                .getParameterName()
                                                                                        == null
                                                                                ? ""
                                                                                : result.getMethodParameter()
                                                                                        .getParameterName(),
                                                                        result.getArgument() == null
                                                                                ? ""
                                                                                : String.valueOf(
                                                                                        result
                                                                                                .getArgument()),
                                                                        error.getDefaultMessage())))
                        .toList();

        ErrorCode errorCode = GlobalErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, details));
    }

    // Redis 연결 실패 혹은 쿼리 타임아웃 시 503(REDIS_CONNECTION_FAILURE) 처리
    @ExceptionHandler({
        org.springframework.data.redis.RedisConnectionFailureException.class,
        org.springframework.dao.QueryTimeoutException.class
    })
    public ResponseEntity<ErrorResponse> handleRedisConnectionFailure(Exception exception) {
        log.error("Redis 통신 오류", exception);
        com.bop.youthpick.auth.exception.AuthErrorCode errorCode =
                com.bop.youthpick.auth.exception.AuthErrorCode.REDIS_CONNECTION_FAILURE;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // 개발자가 예상치 못한 에러 — application_logs 테이블에 남도록 ERROR로 로깅한다.
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
        INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력 형태가 올바르지 않습니다."),
        METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "지원하지 않는 HTTP 메서드입니다."),
        FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "C006", "파일 크기 제한을 초과했습니다."),
        // C003~C006은 file 도메인(FileErrorCode)이 사용 중이라 C007부터 이어간다.
        RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C007", "요청한 리소스를 찾을 수 없습니다."),
        UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C008", "지원하지 않는 요청 형식입니다.");

        private final HttpStatus status;
        private final String code;
        private final String message;
    }
}
