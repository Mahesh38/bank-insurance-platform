package com.bank.persistence.api.internal.v1;

import com.bank.persistence.api.internal.v1.dto.CreateRawPayloadRequest;
import com.bank.persistence.api.internal.v1.dto.RawPayloadResponse;
import com.bank.persistence.entity.RawPayloadEntity;
import com.bank.persistence.service.RawPayloadStore;
import com.bank.persistence.service.RawPayloadStore.RawPayloadView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

@RestController
@RequestMapping("/internal/v1/raw-payloads")
public class RawPayloadController {

    private final RawPayloadStore rawPayloadStore;

    public RawPayloadController(RawPayloadStore rawPayloadStore) {
        this.rawPayloadStore = rawPayloadStore;
    }

    @PostMapping
    public ResponseEntity<RawPayloadResponse> store(@Valid @RequestBody CreateRawPayloadRequest request) {
        byte[] plaintext;
        try {
            plaintext = Base64.getDecoder().decode(request.payloadBase64());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("payloadBase64 is not valid Base64");
        }

        RawPayloadEntity saved = rawPayloadStore.store(
                request.jobId(),
                request.direction(),
                request.operation(),
                request.lob(),
                plaintext,
                request.httpStatus()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toMetadataResponse(saved));
    }

    @GetMapping("/{payloadId}")
    public RawPayloadResponse get(
            @PathVariable String payloadId,
            @RequestParam(name = "decrypt", defaultValue = "false") boolean decrypt) {
        RawPayloadView view = rawPayloadStore.find(payloadId, decrypt)
                .orElseThrow(() -> NotFound.of("RawPayload", payloadId));
        return toResponse(view, decrypt);
    }

    private static RawPayloadResponse toMetadataResponse(RawPayloadEntity e) {
        return RawPayloadResponse.metadata(
                e.getPayloadId(),
                e.getJobId(),
                e.getDirection(),
                e.getOperation(),
                e.getLob(),
                e.getEncryptionKeyId(),
                e.getHttpStatus(),
                e.getCreatedAt(),
                e.getRetainUntil()
        );
    }

    private static RawPayloadResponse toResponse(RawPayloadView view, boolean decrypt) {
        String payloadBase64 = null;
        if (decrypt && view.plaintext() != null) {
            payloadBase64 = Base64.getEncoder().encodeToString(view.plaintext());
        }
        return new RawPayloadResponse(
                view.payloadId(),
                view.jobId(),
                view.direction(),
                view.operation(),
                view.lob(),
                view.encryptionKeyId(),
                view.httpStatus(),
                view.createdAt(),
                view.retainUntil(),
                payloadBase64
        );
    }
}
