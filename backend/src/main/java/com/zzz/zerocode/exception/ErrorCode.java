package com.zzz.zerocode.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    PARAMS_ERROR(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    ACCOUNT_EXISTS(40001, "账号已存在", HttpStatus.CONFLICT),
    LOGIN_FAILED(40002, "账号或密码错误", HttpStatus.BAD_REQUEST),
    NOT_LOGIN_ERROR(40100, "未登录", HttpStatus.UNAUTHORIZED),
    NO_AUTH_ERROR(40300, "无权限", HttpStatus.FORBIDDEN),
    NOT_FOUND_ERROR(40400, "请求资源不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(40500, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(41500, "请求内容类型不支持", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    SYSTEM_ERROR(50000, "系统内部异常", HttpStatus.INTERNAL_SERVER_ERROR),
    OPERATION_ERROR(50001, "操作失败", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
