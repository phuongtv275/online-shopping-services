package com.example.promotionservice.repository;

import com.example.promotionservice.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByProductIdAndIsActiveTrue(Long productId);

    List<Promotion> findByProductId(Long productId);

    Page<Promotion> findByIsActive(Boolean isActive, Pageable pageable);
}
