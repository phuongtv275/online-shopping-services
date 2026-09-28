package com.example.inventoryservice.concurrency;

import com.example.inventoryservice.dto.DeductStockRequest;
import com.example.inventoryservice.dto.DeductStockResponse;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.exception.InsufficientStockException;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class InventoryDistributedLockConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    private static final Long FLASH_SALE_PRODUCT_ID = 8888L;
    private static final int INITIAL_STOCK = 1;
    private static final int CONCURRENT_THREADS = 100;

    @BeforeEach
    void setUp() {
        // Khởi tạo sản phẩm Flash Sale với tồn kho duy nhất = 1
        Inventory inventory = inventoryRepository.findByProductId(FLASH_SALE_PRODUCT_ID)
                .orElse(Inventory.builder()
                        .productId(FLASH_SALE_PRODUCT_ID)
                        .quantity(INITIAL_STOCK)
                        .reservedQuantity(0)
                        .build());
        inventory.setQuantity(INITIAL_STOCK);
        inventoryRepository.saveAndFlush(inventory);
    }

    @Test
    @DisplayName("Kiểm thử 100 luồng đồng thời mua 1 sản phẩm có tồn kho = 1: Duy nhất 1 thành công, 99 thất bại, tồn kho = 0")
    void testConcurrentDeductStock_WithRedissonLock_ShouldPreventOverselling() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(CONCURRENT_THREADS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    // Chờ tín hiệu xuất phát đồng loạt từ startLatch
                    startLatch.await();

                    DeductStockResponse response = inventoryService.deductStock(
                            new DeductStockRequest(FLASH_SALE_PRODUCT_ID, 1));
                    if (response != null && response.getRemainingQuantity() >= 0) {
                        successCount.incrementAndGet();
                    }
                } catch (InsufficientStockException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // LockAcquisitionException hoặc lỗi khác cũng tính vào failure
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Chờ 100 luồng chuẩn bị xong
        readyLatch.await();
        // Bắn phát súng lệnh đồng loạt
        startLatch.countDown();
        // Chờ 100 luồng hoàn tất
        finishLatch.await();
        executorService.shutdown();

        // Kiểm tra kết quả
        System.out.println("====== KẾT QUẢ TEST ĐỒNG THỜI (CONCURRENCY TEST) ======");
        System.out.println("Tổng số luồng đồng thời: " + CONCURRENT_THREADS);
        System.out.println("Số lượng thành công: " + successCount.get());
        System.out.println("Số lượng thất bại (Hết hàng): " + failureCount.get());

        // Nghiệm thu: Duy nhất 1 thành công
        assertEquals(1, successCount.get(), "Chỉ có duy nhất 1 giao dịch thành công vì tồn kho ban đầu = 1");
        // 99 thất bại
        assertEquals(99, failureCount.get(), "99 giao dịch còn lại phải thất bại do hết hàng");

        // Kiểm tra tồn kho trong PostgreSQL
        Inventory finalInventory = inventoryRepository.findByProductId(FLASH_SALE_PRODUCT_ID).orElseThrow();
        System.out.println("Tồn kho còn lại trong PostgreSQL: " + finalInventory.getQuantity());
        assertEquals(0, finalInventory.getQuantity(), "Tồn kho trong CSDL phải chính xác bằng 0, tuyệt đối không bị âm!");
    }
}
