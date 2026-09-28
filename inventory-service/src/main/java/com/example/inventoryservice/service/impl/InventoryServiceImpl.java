package com.example.inventoryservice.service.impl;

import com.example.inventoryservice.dto.InventoryRequest;
import com.example.inventoryservice.dto.InventoryResponse;
import com.example.inventoryservice.dto.PageResponse;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.ProcessedEvent;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.event.OrderItemDto;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.exception.ResourceNotFoundException;
import com.example.inventoryservice.mapper.InventoryMapper;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        log.info("Lấy thông tin tồn kho cho sản phẩm ID: {}", productId);
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tồn kho cho sản phẩm ID: " + productId));
        return inventoryMapper.toDto(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse createOrUpdateInventory(InventoryRequest request) {
        log.info("Thêm hoặc cập nhật tồn kho cho productId: {}, số lượng: {}", request.getProductId(), request.getQuantity());
        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .map(existing -> {
                    existing.setQuantity(request.getQuantity());
                    return existing;
                })
                .orElseGet(() -> inventoryMapper.toEntity(request));

        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> getAllInventories(Pageable pageable) {
        log.info("Lấy danh sách tồn kho phân trang: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<InventoryResponse> page = inventoryRepository.findAll(pageable)
                .map(inventoryMapper::toDto);
        return PageResponse.of(page);
    }

    /**
     * Xử lý sự kiện OrderCreatedEvent từ Kafka:
     * 1. Kiểm tra tính Idempotent qua bảng processed_events bằng eventId (ORDER_CREATED_{orderId}).
     * 2. Nếu đã xử lý rồi -> bỏ qua, tránh trừ trùng kho khi Kafka retry hoặc consumer rebalance.
     * 3. Duyệt qua từng sản phẩm trong đơn, kiểm tra và trừ số lượng tồn kho khả dụng.
     * 4. Lưu lại eventId vào processed_events trong cùng một transaction.
     */
    @Override
    @Transactional
    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        String eventId = "ORDER_CREATED_" + event.getOrderId();
        log.info("Bắt đầu xử lý sự kiện OrderCreatedEvent: eventId={}, orderId={}", eventId, event.getOrderId());

        if (processedEventRepository.existsById(eventId)) {
            log.warn("Sự kiện {} đã được xử lý trước đó. Bỏ qua để đảm bảo tính Idempotent.", eventId);
            return;
        }

        if (event.getItems() != null) {
            for (OrderItemDto item : event.getItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy kho hàng cho sản phẩm ID: " + item.getProductId()));

                if (inventory.getQuantity() < item.getQuantity()) {
                    log.error("Sản phẩm ID {} không đủ tồn kho: hiện có {}, yêu cầu {}",
                            item.getProductId(), inventory.getQuantity(), item.getQuantity());
                    throw new InsufficientStockException(
                            "Sản phẩm " + item.getProductName() + " (ID: " + item.getProductId() + ") không đủ tồn kho!");
                }

                // Trừ tồn kho
                int newQuantity = inventory.getQuantity() - item.getQuantity();
                inventory.setQuantity(newQuantity);
                inventoryRepository.save(inventory);
                log.info("Đã trừ kho cho sản phẩm ID {}: giảm {}, còn lại {}",
                        item.getProductId(), item.getQuantity(), newQuantity);
            }
        }

        // Lưu thông tin event đã xử lý để đảm bảo idempotency
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .processedAt(Instant.now())
                .build();
        processedEventRepository.save(processedEvent);

        log.info("Xử lý thành công trừ kho cho Order ID: {}", event.getOrderId());
    }
}
