package com.bank.insurance.onesb.api;

import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.ProposalSchema;
import com.bank.insurance.onesb.domain.port.inbound.ProposalUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bank proposal API — {@code GET /v1/proposals/schema} (FUNC-004).
 * No Idempotency-Key on GET.
 */
@RestController
@RequestMapping("/v1/proposals")
public class ProposalController {

    private final ProposalUseCase proposalUseCase;

    public ProposalController(ProposalUseCase proposalUseCase) {
        this.proposalUseCase = proposalUseCase;
    }

    @GetMapping("/schema")
    public ResponseEntity<ProposalSchema> getSchema(
            @RequestParam Lob lob,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String manufacturerId,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String quoteJobId) {

        ProposalSchema schema = proposalUseCase.getSchema(
                lob, productCode, manufacturerId, version, quoteJobId);
        return ResponseEntity.ok(schema);
    }
}
