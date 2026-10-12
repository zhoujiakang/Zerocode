package com.zzz.zerocode.common;

import com.zzz.zerocode.exception.ErrorCode;

public final class ResultUtils {
    private ResultUtils() {}
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }
}
