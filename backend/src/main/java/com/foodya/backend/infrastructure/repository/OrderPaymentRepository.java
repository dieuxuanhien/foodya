package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.domain.entities.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, UUID> {
}
