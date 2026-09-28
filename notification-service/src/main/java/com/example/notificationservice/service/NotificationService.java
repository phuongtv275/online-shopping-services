package com.example.notificationservice.service;

import com.example.notificationservice.event.OrderCreatedEvent;

public interface NotificationService {

    void sendOrderConfirmationNotification(OrderCreatedEvent event);
}
