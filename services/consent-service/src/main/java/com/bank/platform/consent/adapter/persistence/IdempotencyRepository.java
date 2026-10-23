package com.bank.platform.consent.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyEntity, String> {}
