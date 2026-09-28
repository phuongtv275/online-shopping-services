package com.example.inventoryservice.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Annotation khai báo áp dụng Redisson Distributed Lock cho phương thức.
 * Hỗ trợ SpEL Expression để xây dựng dynamic lock key từ đối số phương thức.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * SpEL Expression xác định lock key, ví dụ: "'lock:product:' + #request.productId"
     */
    String key();

    /**
     * Thời gian chờ tối đa để giành khóa (mặc định: 3 giây)
     */
    long waitTime() default 3;

    /**
     * Thời hạn tự giải phóng khóa chống Deadlock (mặc định: 5 giây)
     */
    long leaseTime() default 5;

    /**
     * Đơn vị thời gian (mặc định: SECONDS)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
