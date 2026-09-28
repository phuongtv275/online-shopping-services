package com.example.inventoryservice.service.impl;

import com.example.inventoryservice.annotation.DistributedLock;
import com.example.inventoryservice.dto.*;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.ProcessedEvent;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.event.OrderItemDto;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.exception.LockAcquisitionException;
import com.example.inventoryservice.exception.ResourceNotFoundException;
import com.example.inventoryservice.mapper.InventoryMapper;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryMapper inventoryMapper;
    private final RedissonClient redissonClient;

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
     * Trừ tồn kho đồng bộ có bảo vệ bằng Redisson Distributed Lock:
     * 1. Annotation @DistributedLock tự động giành khóa 'lock:product:{productId}' với waitTime=3s, leaseTime=5s.
     * 2. Chỉ duy nhất 1 luồng được phép truy cập critical section tại một thời điểm.
     * 3. Kiểm tra số lượng tồn kho khả dụng trong DB:
     *    - Nếu quantity < requested quantity -> Ném InsufficientStockException.
     *    - Nếu đủ -> Trừ kho và lưu lại CSDL.
     * 4. Giải phóng khóa an toàn tự động trong khối finally của aspect.
     */
    @Override
    @DistributedLock(key = "'lock:product:' + #request.productId", waitTime = 3, leaseTime = 5)
    @Transactional
    public DeductStockResponse deductStock(DeductStockRequest request) {
        log.info("Tiến hành trừ tồn kho trong Critical Section: productId={}, quantity={}",
                request.getProductId(), request.getQuantity());

        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tồn kho cho sản phẩm ID: " + request.getProductId()));

        if (inventory.getQuantity() < request.getQuantity()) {
            log.warn("Sản phẩm ID {} không đủ tồn kho: hiện có {}, yêu cầu {}",
                    request.getProductId(), inventory.getQuantity(), request.getQuantity());
            throw new InsufficientStockException("Sản phẩm đã hết hàng hoặc không đủ số lượng tồn kho khả dụng!");
        }

        int remainingQuantity = inventory.getQuantity() - request.getQuantity();
        inventory.setQuantity(remainingQuantity);
        inventoryRepository.saveAndFlush(inventory);

        log.info("Trừ tồn kho thành công cho sản phẩm ID {}: đã trừ {}, còn lại {}",
                request.getProductId(), request.getQuantity(), remainingQuantity);

        return DeductStockResponse.builder()
                .productId(request.getProductId())
                .deductedQuantity(request.getQuantity())
                .remainingQuantity(remainingQuantity)
                .message("Trừ tồn kho thành công")
                .build();
    }

    /**
     * Xử lý sự kiện OrderCreatedEvent từ Kafka:
     * 1. Kiểm tra tính Idempotent qua bảng processed_events bằng eventId (ORDER_CREATED_{orderId}).
     * 2. Nếu đã xử lý rồi -> bỏ qua, tránh trừ trùng kho khi Kafka retry hoặc consumer rebalance.
     * 3. Sử dụng Redisson Distributed Lock cho từng productId để đảm bảo đồng bộ hóa tuyệt đối
     *    giữa luồng xử lý Kafka và các luồng trừ kho trực tiếp.
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
                String lockKey = "lock:product:" + item.getProductId();
                RLock lock = redissonClient.getLock(lockKey);
                boolean acquired = false;
                try {
                    acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
                    if (!acquired) {
                        throw new LockAcquisitionException("Không thể giành khóa phân tán cho sản phẩm ID: " + item.getProductId());
                    }

                    Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Không tìm thấy kho hàng cho sản phẩm ID: " + item.getProductId()));

                    if (inventory.getQuantity() < item.getQuantity()) {
                        log.error("Sản phẩm ID {} không đủ tồn kho: hiện có {}, yêu cầu {}",
                                item.getProductId(), inventory.getQuantity(), item.getQuantity());
                        throw new InsufficientStockException(
                                "Sản phẩm " + item.getProductName() + " (ID: " + item.getProductId() + ") không đủ tồn kho!");
                    }

                    int newQuantity = inventory.getQuantity() - item.getQuantity();
                    inventory.setQuantity(newQuantity);
                    inventoryRepository.save(inventory);
                    log.info("Đã trừ kho cho sản phẩm ID {}: giảm {}, còn lại {}",
                            item.getProductId(), item.getQuantity(), newQuantity);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LockAcquisitionException("Bị gián đoạn khi xử lý khóa cho sản phẩm ID: " + item.getProductId());
                } finally {
                    if (acquired && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
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
