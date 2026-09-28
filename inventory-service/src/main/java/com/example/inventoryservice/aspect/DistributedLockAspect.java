package com.example.inventoryservice.aspect;

import com.example.inventoryservice.annotation.DistributedLock;
import com.example.inventoryservice.exception.LockAcquisitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP Aspect xử lý phân tán Redisson Lock:
 * 1. @Order(1): Đảm bảo Distributed Lock bao bọc bên ngoài @Transactional. Khóa được lấy
 *    trước khi transaction mở và chỉ được giải phóng sau khi transaction đã COMMIT thành công vào CSDL.
 * 2. Đánh giá SpEL Expression từ tham số phương thức để sinh lock key chính xác.
 * 3. Thực hiện tryLock với waitTime và leaseTime đã khai báo.
 * 4. Tự động giải phóng khóa (Unlock) fail-safe trong khối finally nếu luồng hiện tại đang giữ khóa.
 */
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object handleDistributedLock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String lockKey = parseLockKey(distributedLock.key(), method, joinPoint.getArgs());
        String correlationId = MDC.get("correlationId");

        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        log.info("[cid:{}] [LOCK-ACQUIRE-ATTEMPT] Đang yêu cầu khóa phân tán: key='{}', waitTime={}s, leaseTime={}s",
                correlationId != null ? correlationId : "N/A", lockKey, distributedLock.waitTime(), distributedLock.leaseTime());

        long start = System.currentTimeMillis();
        try {
            acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!acquired) {
                log.warn("[cid:{}] [LOCK-ACQUIRE-FAILED] Không giành được khóa phân tán trong {}s: key='{}'",
                        correlationId != null ? correlationId : "N/A", distributedLock.waitTime(), lockKey);
                throw new LockAcquisitionException("Hệ thống đang bận xử lý giao dịch cho sản phẩm này. Vui lòng thử lại sau!");
            }

            long acquireDuration = System.currentTimeMillis() - start;
            log.info("[cid:{}] [LOCK-ACQUIRED] Giành được khóa thành công sau {} ms: key='{}'",
                    correlationId != null ? correlationId : "N/A", acquireDuration, lockKey);

            return joinPoint.proceed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[cid:{}] Luồng bị gián đoạn khi đang chờ lấy khóa: key='{}'",
                    correlationId != null ? correlationId : "N/A", lockKey, e);
            throw new LockAcquisitionException("Giao dịch bị gián đoạn. Vui lòng thử lại!");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[cid:{}] [LOCK-RELEASED] Đã giải phóng khóa phân tán an toàn: key='{}'",
                        correlationId != null ? correlationId : "N/A", lockKey);
            }
        }
    }

    private String parseLockKey(String keyExpression, Method method, Object[] args) {
        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        try {
            Object evalResult = expressionParser.parseExpression(keyExpression).getValue(context);
            return evalResult != null ? evalResult.toString() : keyExpression;
        } catch (Exception e) {
            log.warn("Không thể phân giải SpEL expression '{}', fallback sử dụng trực tiếp key", keyExpression);
            return keyExpression;
        }
    }
}
