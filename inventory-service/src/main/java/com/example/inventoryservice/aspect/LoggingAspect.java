package com.example.inventoryservice.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP Aspect để ghi log tập trung cho Controller, Service và Kafka Consumers.
 * Tự động ghi lại thời gian thực thi và tham số phục vụ debug.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(com.example.inventoryservice.controller..*) || within(com.example.inventoryservice.service..*) || within(com.example.inventoryservice.consumer..*)")
    public void applicationPackagePointcut() {
    }

    @Around("applicationPackagePointcut()")
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
