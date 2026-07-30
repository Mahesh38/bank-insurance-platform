package com.bank.insurance.onesb.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.ProposalSchema;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.port.inbound.ProposalUseCase;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbProposalPort;
import com.bank.insurance.onesb.lob.LobProposalHandler;
import com.bank.insurance.onesb.lob.LobProposalHandlerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Proposal schema orchestration (FUNC-004): optional quote expiry check → LOB path → 1SB GET
 * (schema cache lives in the 1SB proposal adapter).
 */
@Service
public class ProposalService implements ProposalUseCase {

    private final JobStorePort jobStore;
    private final LobProposalHandlerRegistry handlerRegistry;
    private final OneSbProposalPort proposalPort;

    public ProposalService(JobStorePort jobStore,
                           LobProposalHandlerRegistry handlerRegistry,
                           OneSbProposalPort proposalPort) {
        this.jobStore = jobStore;
        this.handlerRegistry = handlerRegistry;
        this.proposalPort = proposalPort;
    }

    @Override
    public ProposalSchema getSchema(Lob lob, String productCode, String manufacturerId,
                                    String version, String quoteJobId) {
        if (lob == null) {
            throw new ServiceException(ServiceErrorResponse.builder()
                    .title("Validation Failed")
                    .status(422)
                    .detail("lob is required")
                    .code(ErrorCodes.VALIDATION_ERROR)
                    .retryable(false)
                    .build());
        }
        if (StringUtils.hasText(quoteJobId)) {
            assertQuoteUsable(quoteJobId);
        }

        LobProposalHandler handler = handlerRegistry.get(lob);
        String path = handler.schemaPath(productCode, manufacturerId, version);
        return proposalPort.getSchema(lob, productCode, manufacturerId, version, path);
    }

    private void assertQuoteUsable(String quoteJobId) {
        Optional<QuoteJob> found = jobStore.findQuoteJob(quoteJobId);
        if (found.isEmpty()) {
            throw quoteExpired("Quote job not found or expired: " + quoteJobId);
        }
        QuoteJob job = found.get();
        JobStatus status = job.status();
        if (status == JobStatus.TIMEOUT || status == JobStatus.FAILED) {
            throw quoteExpired("Quote job is " + status + ": " + quoteJobId);
        }
        if ((status == JobStatus.COMPLETED || status == JobStatus.PARTIAL)
                && (job.offers() == null || job.offers().isEmpty())) {
            throw quoteExpired("Quote job has no offers: " + quoteJobId);
        }
    }

    private static ServiceException quoteExpired(String detail) {
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Quote Expired")
                .status(410)
                .detail(detail)
                .code(ErrorCodes.QUOTE_EXPIRED)
                .retryable(false)
                .build());
    }
}
