package com.bank.insurance.persistence.persistence.repo;

import com.bank.insurance.persistence.persistence.entity.PaymentSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSessionRepository extends JpaRepository<PaymentSessionEntity, String> {
}
