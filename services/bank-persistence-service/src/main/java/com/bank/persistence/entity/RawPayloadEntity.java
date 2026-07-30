package com.bank.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "raw_payload")
@Getter
@Setter
@NoArgsConstructor
public class RawPayloadEntity {

    @Id
    @Column(name = "payload_id", length = 36, nullable = false)
    private String payloadId;

    @Column(name = "job_id", length = 36, nullable = false)
    private String jobId;

    @Column(name = "direction", length = 3, nullable = false)
    private String direction;

    @Column(name = "operation", length = 100, nullable = false)
    private String operation;

    @Column(name = "lob", length = 20, nullable = false)
    private String lob;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "payload_enc", nullable = false)
    private byte[] payloadEnc;

    @Column(name = "encryption_key_id", length = 50, nullable = false)
    private String encryptionKeyId;

    @Column(name = "http_status")
    private Short httpStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "retain_until", nullable = false)
    private LocalDate retainUntil;
}
