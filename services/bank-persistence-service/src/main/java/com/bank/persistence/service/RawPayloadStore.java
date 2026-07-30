package com.bank.persistence.service;

import com.bank.persistence.config.PayloadEncryptionProperties;
import com.bank.persistence.crypto.AesGcmPayloadCipher;
import com.bank.persistence.entity.RawPayloadEntity;
import com.bank.persistence.repo.RawPayloadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Sole writer/reader of {@code raw_payload} encrypted bytes.
 * Callers never touch {@link RawPayloadRepository} for payload content.
 */
@Service
public class RawPayloadStore {

    private final RawPayloadRepository repository;
    private final AesGcmPayloadCipher cipher;
    private final PayloadEncryptionProperties encryptionProperties;

    public RawPayloadStore(
            RawPayloadRepository repository,
            AesGcmPayloadCipher cipher,
            PayloadEncryptionProperties encryptionProperties) {
        this.repository = repository;
        this.cipher = cipher;
        this.encryptionProperties = encryptionProperties;
    }

    @Transactional
    public RawPayloadEntity store(
            String jobId,
            String direction,
            String operation,
            String lob,
            byte[] plaintextBytes,
            Short httpStatus) {
        Instant now = Instant.now();
        LocalDate retainUntil = LocalDate.now().plusYears(encryptionProperties.retainYearsOrDefault());

        RawPayloadEntity entity = new RawPayloadEntity();
        entity.setPayloadId(UUID.randomUUID().toString());
        entity.setJobId(jobId);
        entity.setDirection(direction);
        entity.setOperation(operation);
        entity.setLob(lob);
        entity.setPayloadEnc(cipher.encrypt(plaintextBytes));
        entity.setEncryptionKeyId(encryptionProperties.keyId());
        entity.setHttpStatus(httpStatus);
        entity.setCreatedAt(now);
        entity.setRetainUntil(retainUntil);
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<RawPayloadView> find(String payloadId, boolean decrypt) {
        return repository.findById(payloadId).map(entity -> toView(entity, decrypt));
    }

    private RawPayloadView toView(RawPayloadEntity entity, boolean decrypt) {
        byte[] plaintext = decrypt ? cipher.decrypt(entity.getPayloadEnc()) : null;
        return new RawPayloadView(
                entity.getPayloadId(),
                entity.getJobId(),
                entity.getDirection(),
                entity.getOperation(),
                entity.getLob(),
                entity.getEncryptionKeyId(),
                entity.getHttpStatus(),
                entity.getCreatedAt(),
                entity.getRetainUntil(),
                plaintext
        );
    }

    /**
     * Metadata view; {@code plaintext} is non-null only when {@code decrypt=true}.
     */
    public record RawPayloadView(
            String payloadId,
            String jobId,
            String direction,
            String operation,
            String lob,
            String encryptionKeyId,
            Short httpStatus,
            Instant createdAt,
            LocalDate retainUntil,
            byte[] plaintext
    ) {}
}
