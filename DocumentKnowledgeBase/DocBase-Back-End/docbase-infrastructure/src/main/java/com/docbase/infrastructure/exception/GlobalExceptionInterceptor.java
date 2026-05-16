package com.docbase.infrastructure.exception;

import com.docbase.common.core.dto.ResponseDTO;
import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.common.exception.error.ErrorCode.Business;
import com.docbase.common.exception.error.ErrorCode.Client;
import com.docbase.common.exception.error.ErrorCode.Internal;
import com.google.common.util.concurrent.UncheckedExecutionException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception interceptor for MVC handlers.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionInterceptor {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseDTO<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.error("request '{}', permission denied '{}'", request.getRequestURI(), e.getMessage());
        return ResponseDTO.fail(new ApiException(Business.PERMISSION_NOT_ALLOWED_TO_OPERATE));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseDTO<?> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
        HttpServletRequest request) {
        log.error("request '{}', unsupported method '{}'", request.getRequestURI(), e.getMethod());
        return ResponseDTO.fail(new ApiException(Client.COMMON_REQUEST_METHOD_INVALID, e.getMethod()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseDTO<?> handleServiceException(ApiException e) {
        log.error(e.getMessage(), e);
        return ResponseDTO.fail(e, e.getPayload());
    }

    @ExceptionHandler(UncheckedExecutionException.class)
    public ResponseDTO<?> handleServiceException(UncheckedExecutionException e) {
        log.error(e.getMessage(), e);
        return ResponseDTO.fail(new ApiException(Internal.GET_CACHE_FAILED, e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseDTO<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String errorMsg = String.format("request '%s', runtime exception occurred.", request.getRequestURI());
        log.error(errorMsg, e);
        return ResponseDTO.fail(new ApiException(Internal.INTERNAL_ERROR, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseDTO<?> handleException(Exception e, HttpServletRequest request) {
        String errorMsg = String.format("request '%s', unknown exception occurred.", request.getRequestURI());
        log.error(errorMsg, e);
        return ResponseDTO.fail(new ApiException(Internal.INTERNAL_ERROR, e.getMessage()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseDTO<?> handleBindException(BindException e) {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return ResponseDTO.fail(new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseDTO<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseDTO.fail(new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, message));
    }
}
