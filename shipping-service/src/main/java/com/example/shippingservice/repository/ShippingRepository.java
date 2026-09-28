package com.example.shippingservice.repository;

import com.example.shippingservice.entity.Shipping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingRepository extends JpaRepository<Shipping, Long> {

    Optional<Shipping> findByOrderId(Long orderId);

    Optional<Shipping> findByTrackingNumber(String trackingNumber);

    boolean existsByOrderId(Long orderId);
}
