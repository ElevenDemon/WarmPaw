package com.nuanzhua.pet.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;

/** 记录控制器方法、执行结果和耗时，方便定位请求处理情况。 */
@Aspect
@Component
public class RequestTimingAspect {
    private static final Logger log=LoggerFactory.getLogger(RequestTimingAspect.class);
    /** 记录接口执行耗时，不记录密码、令牌和请求体。 */
    @Around("execution(public * com.nuanzhua.pet.controller..*(..))")
    public Object measure(ProceedingJoinPoint point) throws Throwable {
        long start=System.nanoTime();
        try {
            Object result=point.proceed();
            log.info("接口执行成功 method={} durationMs={}",point.getSignature().toShortString(),(System.nanoTime()-start)/1_000_000);
            return result;
        } catch (Throwable error) {
            log.warn("接口执行失败 method={} exception={} durationMs={}",point.getSignature().toShortString(),error.getClass().getSimpleName(),(System.nanoTime()-start)/1_000_000);
            throw error;
        }
    }
}
