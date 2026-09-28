package com.example.notificationservice.service;

import com.example.notificationservice.event.OrderCreatedEvent;
import com.example.notificationservice.event.ShippingStatusEvent;

public interface NotificationService {

    void sendOrderConfirmationNotification(OrderCreatedEvent event);

    void sendDeliverySuccessNotification(ShippingStatusEvent event);
}
