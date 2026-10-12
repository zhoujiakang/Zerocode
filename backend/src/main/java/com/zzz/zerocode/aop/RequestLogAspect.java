package com.zzz.zerocode.aop;

import cn.hutool.core.util.IdUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 只记录方法和耗时，避免在日志中写入密码、提示词等请求正文。 */
@Slf4j
@Aspect
@Component
public class RequestLogAspect {
    @Around("execution(* com.zzz.zerocode.controller..*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = IdUtil.fastSimpleUUID();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletResponse response = attributes.getResponse();
            if (response != null) response.setHeader("X-Request-Id", requestId);
        }
        long start = System.nanoTime();
        try { return joinPoint.proceed(); }
        finally {
            log.info("requestId={} method={} durationMs={}", requestId,
                    joinPoint.getSignature().toShortString(), (System.nanoTime() - start) / 1_000_000);
        }
    }
}
