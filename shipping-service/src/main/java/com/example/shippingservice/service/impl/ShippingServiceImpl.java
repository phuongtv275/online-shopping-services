package com.example.shippingservice.service.impl;

import com.example.shippingservice.client.OrderServiceClient;
import com.example.shippingservice.dto.ApiResponse;
import com.example.shippingservice.dto.CreateShippingRequest;
import com.example.shippingservice.dto.DeliverShippingRequest;
import com.example.shippingservice.dto.PageResponse;
import com.example.shippingservice.dto.ShippingResponse;
import com.example.shippingservice.entity.Shipping;
import com.example.shippingservice.event.ShippingStatusEvent;
import com.example.shippingservice.exception.InvalidShippingStateException;
import com.example.shippingservice.exception.ResourceNotFoundException;
import com.example.shippingservice.mapper.ShippingMapper;
import com.example.shippingservice.producer.ShippingEventProducer;
import com.example.shippingservice.repository.ShippingRepository;
import com.example.shippingservice.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingServiceImpl implements ShippingService {

    private final ShippingRepository shippingRepository;
    private final ShippingMapper shippingMapper;
    private final ShippingEventProducer shippingEventProducer;
    private final OrderServiceClient orderServiceClient;

    /**
     * Tạo mới một vận đơn giao hàng.
     * Quy trình nghiệp vụ:
     * 1. Xác thực sự tồn tại của đơn hàng qua OpenFeign gọi sang order-service (Resilience4j CircuitBreaker bảo vệ).
     * 2. Kiểm tra tính đơn nhất (1 đơn hàng chỉ có 1 vận đơn tương ứng).
     * 3. Sinh mã trackingNumber duy nhất theo định dạng: SHIP-yyyyMMdd-UUID8.
     * 4. Lưu trạng thái ban đầu là PREPARING.
     */
    @Transactional
    @Override
    public ShippingResponse createShipping(CreateShippingRequest request) {
        log.info("Bắt đầu xử lý tạo vận đơn cho đơn hàng ID: {}", request.getOrderId());

        // 1. Xác thực đơn hàng tồn tại qua OpenFeign
        ApiResponse<Map<String, Object>> orderApiResponse = orderServiceClient.getOrderById(request.getOrderId());
        if (orderApiResponse == null || orderApiResponse.getData() == null) {
            log.error("Không tìm thấy thông tin đơn hàng ID: {} từ order-service", request.getOrderId());
            throw new ResourceNotFoundException("Đơn hàng với ID " + request.getOrderId() + " không tồn tại");
        }

        // 2. Kiểm tra nếu đơn hàng đã được lập vận đơn
        if (shippingRepository.existsByOrderId(request.getOrderId())) {
            log.warn("Đơn hàng ID {} đã có vận đơn trong hệ thống", request.getOrderId());
            throw new InvalidShippingStateException("Đơn hàng ID " + request.getOrderId() + " đã được tạo vận đơn trước đó!");
        }

        // 3. Tạo mã tracking number duy nhất
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String trackingNumber = String.format("SHIP-%s-%s", datePrefix, uniqueSuffix);

        Shipping shipping = shippingMapper.toEntity(request);
        shipping.setTrackingNumber(trackingNumber);
        shipping.setStatus("PREPARING");

        Shipping savedShipping = shippingRepository.save(shipping);
        log.info("Tạo vận đơn thành công: ID={}, trackingNumber={}, orderId={}",
                savedShipping.getId(), savedShipping.getTrackingNumber(), savedShipping.getOrderId());

        return shippingMapper.toDto(savedShipping);
    }

    /**
     * Shipper xác nhận đã giao hàng thành công.
     * Quy trình Choreography:
     * 1. Tìm bản ghi vận đơn và kiểm tra trạng thái hiện tại.
     * 2. Cập nhật trạng thái thành DELIVERED và ghi nhận thời điểm deliveredAt.
     * 3. Lưu vào CSDL PostgreSQL shipping_db.
     * 4. Bắn sự kiện ShippingStatusEvent (status=DELIVERED) vào Kafka topic 'shipping-events'.
     *    Sự kiện này sẽ được order-service (cập nhật COMPLETED) và notification-service (gửi SMS/Email)
     *    tiêu thụ độc lập theo mô hình Event-Driven Choreography mà không cần gọi API đồng bộ.
     */
    @Transactional
    @Override
    public ShippingResponse deliverShipping(Long id, DeliverShippingRequest request) {
        log.info("Shipper xác nhận giao hàng cho vận đơn ID: {}", id);

        Shipping shipping = shippingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn với ID: " + id));

        if ("DELIVERED".equalsIgnoreCase(shipping.getStatus())) {
            log.warn("Vận đơn ID {} đã ở trạng thái DELIVERED từ trước lúc {}", id, shipping.getDeliveredAt());
            throw new InvalidShippingStateException("Vận đơn ID " + id + " đã được giao thành công trước đó!");
        }

        Instant deliveryTime = Instant.now();
        shipping.setStatus("DELIVERED");
        shipping.setDeliveredAt(deliveryTime);

        if (request != null && request.getDeliveryNote() != null && !request.getDeliveryNote().isBlank()) {
            String combinedNote = shipping.getNote() != null ? shipping.getNote() + " | " + request.getDeliveryNote() : request.getDeliveryNote();
            shipping.setNote(combinedNote);
        }

        Shipping updatedShipping = shippingRepository.save(shipping);
        log.info("Đã cập nhật trạng thái vận đơn ID {} sang DELIVERED thành công trong CSDL", id);

        // Phát sự kiện vào Kafka để kích hoạt chu trình Choreography
        ShippingStatusEvent event = shippingMapper.toEvent(updatedShipping);
        event.setCorrelationId(MDC.get("correlationId"));
        event.setTimestamp(Instant.now());
        shippingEventProducer.publishShippingStatusEvent(event);

        return shippingMapper.toDto(updatedShipping);
    }

    @Override
    public ShippingResponse getShippingById(Long id) {
        log.info("Tra cứu thông tin vận đơn ID: {}", id);
        Shipping shipping = shippingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn với ID: " + id));
        return shippingMapper.toDto(shipping);
    }

    @Override
    public ShippingResponse getShippingByOrderId(Long orderId) {
        log.info("Tra cứu thông tin vận đơn theo orderId: {}", orderId);
        Shipping shipping = shippingRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vận đơn cho đơn hàng ID: " + orderId));
        return shippingMapper.toDto(shipping);
    }

    @Override
    public PageResponse<ShippingResponse> getAllShippings(Pageable pageable) {
        log.info("Lấy danh sách vận đơn phân trang: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Shipping> page = shippingRepository.findAll(pageable);
        return PageResponse.of(page.map(shippingMapper::toDto));
    }
}
