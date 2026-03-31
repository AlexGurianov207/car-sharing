package com.example.carsharing.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ServiceExecutionTimeAspect {

    @Around("execution(* com.example.carsharing.service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            long durationMs = System.currentTimeMillis() - start;
            log.info("[AOP] {} completed in {} ms", method, durationMs);
            return result;
        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.warn("[AOP] {} failed in {} ms: {}", method, durationMs, ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
