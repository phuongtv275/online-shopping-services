package com.example.notificationservice.service.impl;

import com.example.notificationservice.event.OrderCreatedEvent;
import com.example.notificationservice.event.OrderItemDto;
import com.example.notificationservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    /**
     * Giả lập gửi email xác nhận và hóa đơn điện tử cho khách hàng:
     * 1. Định dạng nội dung chi tiết hóa đơn (sản phẩm, số lượng, đơn giá, tổng tiền, địa chỉ giao hàng).
     * 2. Ghi log hoàn tất gửi email kèm correlationId trong MDC.
     */
    @Override
    public void sendOrderConfirmationNotification(OrderCreatedEvent event) {
        StringBuilder invoiceBuilder = new StringBuilder();
        invoiceBuilder.append("\n=================== HÓA ĐƠN ĐIỆN TỬ ===================\n");
        invoiceBuilder.append("Mã đơn hàng: #").append(event.getOrderId()).append("\n");
        invoiceBuilder.append("Khách hàng ID: ").append(event.getCustomerId()).append("\n");
        invoiceBuilder.append("Email nhận: ").append(event.getCustomerEmail()).append("\n");
        invoiceBuilder.append("Địa chỉ giao: ").append(event.getShippingAddress()).append("\n");
        invoiceBuilder.append("-------------------------------------------------------\n");
        invoiceBuilder.append("DANH SÁCH SẢN PHẨM:\n");

        if (event.getItems() != null) {
            for (OrderItemDto item : event.getItems()) {
                invoiceBuilder.append(String.format(" - %s (x%d): %,.2f VND\n",
                        item.getProductName(), item.getQuantity(), item.getUnitPrice()));
            }
        }

        invoiceBuilder.append("-------------------------------------------------------\n");
        invoiceBuilder.append(String.format("TỔNG TIỀN THANH TOÁN: %,.2f VND\n", event.getTotalAmount()));
        invoiceBuilder.append("Trạng thái đơn: ").append(event.getStatus()).append("\n");
        invoiceBuilder.append("=======================================================");

        log.info(invoiceBuilder.toString());
        log.info("Đã gửi email xác nhận đơn hàng #{} thành công tới địa chỉ: {}",
                event.getOrderId(), event.getCustomerEmail());
    }
}
