package com.example.promotionservice.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP Aspect để ghi log tập trung cho Controller và Service của promotion-service.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(com.example.promotionservice.controller..*) || within(com.example.promotionservice.service..*)")
    public void promotionPackagePointcut() {
    }

    @Around("promotionPackagePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = MDC.get("correlationId");
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("[cid:{}] >> Enter: {}.{}() with arguments = {}",
                correlationId != null ? correlationId : "N/A", className, methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - start;
            log.info("[cid:{}] << Exit: {}.{}() executed in {} ms",
                    correlationId != null ? correlationId : "N/A", className, methodName, elapsedTime);
            return result;
        } catch (Throwable ex) {
            long elapsedTime = System.currentTimeMillis() - start;
            log.error("[cid:{}] !! Exception in {}.{}() after {} ms with message: {}",
                    correlationId != null ? correlationId : "N/A", className, methodName, elapsedTime, ex.getMessage());
            throw ex;
        }
    }
}
