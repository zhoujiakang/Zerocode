package com.zzz.zerocode.exception;

public final class ThrowUtils {
    private ThrowUtils() {}
    public static void throwIf(boolean condition, RuntimeException exception) {
        if (condition) throw exception;
    }
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
