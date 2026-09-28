package com.example.inventoryservice.runner;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    @Override
    public void run(String... args) {
        if (inventoryRepository.count() == 0) {
            log.info("Khởi tạo dữ liệu tồn kho ban đầu...");
            List<Inventory> initialInventories = List.of(
                    Inventory.builder().productId(1L).quantity(50).reservedQuantity(0).build(),
                    Inventory.builder().productId(2L).quantity(100).reservedQuantity(0).build(),
                    Inventory.builder().productId(3L).quantity(30).reservedQuantity(0).build(),
                    Inventory.builder().productId(4L).quantity(75).reservedQuantity(0).build(),
                    Inventory.builder().productId(5L).quantity(20).reservedQuantity(0).build()
            );
            inventoryRepository.saveAll(initialInventories);
            log.info("Đã khởi tạo tồn kho mẫu cho {} sản phẩm.", initialInventories.size());
        }
    }
}
