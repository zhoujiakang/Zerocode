package com.zzz.zerocode.exception;

import com.zzz.zerocode.common.BaseResponse;
import com.zzz.zerocode.common.ResultUtils;
import jakarta.validation.ConstraintViolationException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.NotPermissionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotLogin(NotLoginException e) { return failure(ErrorCode.NOT_LOGIN_ERROR); }
    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<BaseResponse<Void>> handleNoPermission(Exception e) { return failure(ErrorCode.NO_AUTH_ERROR); }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("Business error: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ResultUtils.error(e.getCode(), e.getMessage()));
    }
    @ExceptionHandler(BindException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidation(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()).findFirst().orElse("请求参数错误");
        return ResponseEntity.badRequest().body(ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message));
    }
    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<BaseResponse<Void>> handleBadRequest(Exception e) {
        return failure(ErrorCode.PARAMS_ERROR);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFound(Exception e) { return failure(ErrorCode.NOT_FOUND_ERROR); }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethod(Exception e) { return failure(ErrorCode.METHOD_NOT_ALLOWED); }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMediaType(Exception e) { return failure(ErrorCode.UNSUPPORTED_MEDIA_TYPE); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled request error", e);
        return failure(ErrorCode.SYSTEM_ERROR);
    }
    private ResponseEntity<BaseResponse<Void>> failure(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ResultUtils.error(errorCode));
    }
}
