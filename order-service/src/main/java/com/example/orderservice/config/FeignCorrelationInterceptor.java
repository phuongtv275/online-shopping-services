package com.example.orderservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Feign RequestInterceptor để tự động chuyển tiếp X-Correlation-Id
 * từ context hiện tại (MDC) sang các microservice gọi kế tiếp qua OpenFeign.
 */
@Component
public class FeignCorrelationInterceptor implements RequestInterceptor {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            template.header(CORRELATION_ID_HEADER, correlationId);
        }
    }
}
