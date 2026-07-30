package com.bank.persistence.repo;

import com.bank.persistence.entity.PaymentSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSessionRepository extends JpaRepository<PaymentSessionEntity, String> {
}
