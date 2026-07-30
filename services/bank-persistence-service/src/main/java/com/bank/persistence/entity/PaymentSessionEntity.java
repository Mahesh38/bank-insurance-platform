package com.bank.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "payment_session")
@Getter
@Setter
@NoArgsConstructor
public class PaymentSessionEntity {

    @Id
    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId;

    @Column(name = "job_id", length = 36)
    private String jobId;

    @Column(name = "application_number", length = 50, nullable = false)
    private String applicationNumber;

    @Column(name = "lob", length = 20, nullable = false)
    private String lob;

    @Column(name = "payment_url", nullable = false, columnDefinition = "TEXT")
    private String paymentUrl;

    @Column(name = "redirect_url", nullable = false, columnDefinition = "TEXT")
    private String redirectUrl;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "external_txn_id", length = 100)
    private String externalTxnId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by_actor", length = 100, nullable = false)
    private String createdByActor;
}
