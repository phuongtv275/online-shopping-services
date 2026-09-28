package com.example.orderservice.service.impl;

import com.example.orderservice.client.ProductServiceClient;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.producer.OrderEventProducer;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;

    /**
     * Tạo đơn hàng mới:
     * 1. Duyệt qua từng sản phẩm trong giỏ hàng, gọi OpenFeign sang product-service
     *    để kiểm tra sự tồn tại và lấy giá niêm yết chính xác từ server (tránh gian lận giá từ client).
     * 2. Bọc bởi Resilience4j Circuit Breaker trên ProductServiceClient để tự động chuyển sang Fallback
     *    nếu product-service bị timeout, lỗi mạng hoặc sập server.
     * 3. Tính toán tổng tiền chính xác, lưu đơn hàng vào CSDL với trạng thái ban đầu là PENDING.
     * 4. Bắn sự kiện bất đồng bộ OrderCreatedEvent vào Kafka topic 'order-events' kèm correlationId.
     * 5. Trả kết quả về cho client ngay lập tức (<50ms).
     */
    @Override
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        log.info("Bắt đầu xử lý tạo đơn hàng cho khách hàng ID: {}, email: {}",
                request.getCustomerId(), request.getCustomerEmail());

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .shippingAddress(request.getShippingAddress())
                .status("PENDING")
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal calculatedTotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {
            log.info("Gọi OpenFeign lấy thông tin sản phẩm ID: {}", itemReq.getProductId());
            ApiResponse<ProductResponse> productApiResponse = productServiceClient.getProductById(itemReq.getProductId());

            if (productApiResponse == null || productApiResponse.getData() == null) {
                log.error("Không tìm thấy thông tin sản phẩm ID: {}", itemReq.getProductId());
                throw new ResourceNotFoundException("Sản phẩm ID " + itemReq.getProductId() + " không tồn tại!");
            }

            ProductResponse product = productApiResponse.getData();
            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            calculatedTotal = calculatedTotal.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(calculatedTotal);
        Order savedOrder = orderRepository.save(order);
        log.info("Lưu đơn hàng thành công vào DB với ID: {}, tổng tiền: {}", savedOrder.getId(), savedOrder.getTotalAmount());

        // Chuẩn bị và phát sự kiện sang Kafka
        OrderCreatedEvent event = orderMapper.toEvent(savedOrder);
        event.setCorrelationId(MDC.get("correlationId"));
        orderEventProducer.publishOrderCreatedEvent(event);

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.info("Lấy thông tin chi tiết đơn hàng ID: {}", id);
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + id));
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        log.info("Lấy danh sách đơn hàng phân trang: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<OrderResponse> page = orderRepository.findAll(pageable)
                .map(orderMapper::toDto);
        return PageResponse.of(page);
    }
}
