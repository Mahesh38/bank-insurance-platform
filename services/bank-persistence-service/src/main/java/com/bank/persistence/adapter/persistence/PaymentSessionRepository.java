package com.bank.persistence.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSessionRepository extends JpaRepository<PaymentSessionEntity, String> {}
