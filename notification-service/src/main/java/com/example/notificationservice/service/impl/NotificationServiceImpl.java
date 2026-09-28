package com.example.notificationservice.service.impl;

import com.example.notificationservice.event.OrderCreatedEvent;
import com.example.notificationservice.event.OrderItemDto;
import com.example.notificationservice.event.ShippingStatusEvent;
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

    /**
     * Giả lập gửi tin nhắn/email chúc mừng khách hàng khi Shipper giao hàng thành công:
     * Được kích hoạt tự động qua Kafka Event-Driven Choreography từ topic 'shipping-events'.
     */
    @Override
    public void sendDeliverySuccessNotification(ShippingStatusEvent event) {
        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append("\n=================== THÔNG BÁO GIAO HÀNG THÀNH CÔNG ===================\n");
        msgBuilder.append("CHÚC MỪNG QUÝ KHÁCH HÀNG!\n");
        msgBuilder.append("Đơn hàng #").append(event.getOrderId()).append(" đã được giao thành công!\n");
        msgBuilder.append("Mã vận đơn: ").append(event.getTrackingNumber()).append("\n");
        msgBuilder.append("Shipper thực hiện: ").append(event.getShipperName()).append(" (SĐT: ").append(event.getShipperPhone()).append(")\n");
        msgBuilder.append("Địa chỉ giao nhận: ").append(event.getShippingAddress()).append("\n");
        msgBuilder.append("Thời gian giao hàng: ").append(event.getDeliveredAt()).append("\n");
        if (event.getNote() != null && !event.getNote().isBlank()) {
            msgBuilder.append("Ghi chú: ").append(event.getNote()).append("\n");
        }
        msgBuilder.append("Cảm ơn quý khách đã tin tưởng và mua sắm tại cửa hàng!\n");
        msgBuilder.append("======================================================================");

        log.info(msgBuilder.toString());
        log.info("CHOREOGRAPHY: Đã gửi thông báo giao hàng thành công cho đơn hàng #{} (Mã vận đơn: {})",
                event.getOrderId(), event.getTrackingNumber());
    }
}
