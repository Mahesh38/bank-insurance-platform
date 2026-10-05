package com.bank.workforce.bff.lead;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process Lead stand-in so the BFF can be exercised without lead-service.
 * Production identity minting stays in lead-service ({@code D-020}).
 */
@Component
@ConditionalOnProperty(name = "workforce.downstream.lead-mode", havingValue = "stub", matchIfMissing = true)
public class StubLeadGateway implements LeadGateway {

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, LeadRecord> byId = new ConcurrentHashMap<>();
    private final ServiceErrors errors;

    public StubLeadGateway(ServiceErrors errors) {
        this.errors = errors;
    }

    @Override
    public LeadRecord create(CreateLeadCommand command) {
        for (LeadRecord existing : byId.values()) {
            if (command.actorId().equals(existing.assignedRmId())
                || command.actorId().equals(principal(existing))) {
                if (command.customerId().equals(existing.customerId())
                    && command.productClass().equals(existing.productClass())
                    && "NEW".equals(existing.state())) {
                    throw errors.error(ErrorCodes.CONFLICT)
                        .component("StubLeadGateway")
                        .operation("create")
                        .reason("unfinished lead exists")
                        .build();
                }
            }
        }
        Instant now = Instant.now();
        String leadId = mintUlid();
        LeadRecord created = new LeadRecord(
            leadId,
            command.customerId(),
            command.lob(),
            command.productClass(),
            command.branchId(),
            "NEW",
            "NEW",
            command.actorId(),
            null,
            false,
            "NOT_EVALUATED",
            false,
            leadId,
            now,
            now);
        byId.put(leadId, created);
        return created;
    }

    @Override
    public LeadRecord get(String leadId, String actorId) {
        LeadRecord lead = byId.get(leadId);
        if (lead == null || !visible(lead, actorId)) {
            throw errors.error(ErrorCodes.RESOURCE_NOT_FOUND)
                .component("StubLeadGateway")
                .operation("get")
                .reason("lead is absent from this book")
                .build();
        }
        return lead;
    }

    @Override
    public List<LeadRecord> list(String actorId) {
        List<LeadRecord> items = new ArrayList<>();
        for (LeadRecord lead : byId.values()) {
            if (visible(lead, actorId)) {
                items.add(lead);
            }
        }
        return List.copyOf(items);
    }

    @Override
    public LeadRecord startOnboarding(String leadId, String actorId) {
        LeadRecord lead = get(leadId, actorId);
        String token = lead.customerId().toUpperCase(Locale.ROOT);
        String outcome = "PASS";
        boolean hold = false;
        boolean required = false;
        if (token.contains("BLOCK")) {
            throw errors.error(ErrorCodes.ILLEGAL_TRANSITION)
                .component("StubLeadGateway")
                .operation("startOnboarding")
                .reason("exception rules blocked onboarding")
                .build();
        }
        if (token.contains("HOLD")) {
            outcome = "APPROVAL_REQUIRED";
            hold = true;
            required = true;
        }
        LeadRecord updated = copy(lead, lead.state(), lead.assignedRmId(), lead.assignedSpId(),
            hold, outcome, required);
        byId.put(leadId, updated);
        return updated;
    }

    @Override
    public LeadRecord assign(AssignLeadCommand command) {
        LeadRecord lead = get(command.leadId(), command.actorId());
        if ("NOT_EVALUATED".equals(lead.exceptionOutcome())) {
            throw errors.error(ErrorCodes.ILLEGAL_TRANSITION)
                .component("StubLeadGateway")
                .operation("assign")
                .reason("start onboarding before assignment")
                .build();
        }
        if (lead.exceptionHold() || "BLOCK".equals(lead.exceptionOutcome())) {
            throw errors.error(ErrorCodes.CONFLICT)
                .component("StubLeadGateway")
                .operation("assign")
                .reason("exception hold is active")
                .build();
        }
        if ("UNCERTIFIED".equalsIgnoreCase(command.assignedRmId())) {
            throw errors.error(ErrorCodes.SP_CERTIFICATION_REQUIRED)
                .component("StubLeadGateway")
                .operation("assign")
                .reason("assignee must hold a valid SP certification")
                .build();
        }
        LeadRecord updated = copy(lead, "ASSIGNED", command.assignedRmId(), command.assignedSpId(),
            false, lead.exceptionOutcome(), false);
        byId.put(command.leadId(), updated);
        return updated;
    }

    private static boolean visible(LeadRecord lead, String actorId) {
        return actorId.equals(lead.assignedRmId()) || actorId.equals(principal(lead));
    }

    private static String principal(LeadRecord lead) {
        return lead.assignedRmId();
    }

    private static LeadRecord copy(
            LeadRecord lead,
            String state,
            String assignedRmId,
            String assignedSpId,
            boolean hold,
            String outcome,
            boolean required) {
        return new LeadRecord(
            lead.leadId(),
            lead.customerId(),
            lead.lob(),
            lead.productClass(),
            lead.branchId(),
            state,
            "ASSIGNED".equals(state) ? "NEW" : lead.dashboardStage(),
            assignedRmId,
            assignedSpId,
            hold,
            outcome,
            required,
            lead.journeyId(),
            lead.createdAt(),
            Instant.now());
    }

    private static String mintUlid() {
        long timestamp = Instant.now().toEpochMilli();
        char[] buffer = new char[26];
        long remaining = timestamp;
        for (int i = 9; i >= 0; i--) {
            buffer[i] = ALPHABET.charAt((int) (remaining & 31));
            remaining >>>= 5;
        }
        byte[] entropy = new byte[10];
        RANDOM.nextBytes(entropy);
        long bits = 0;
        int bitCount = 0;
        int index = 10;
        for (byte b : entropy) {
            bits = (bits << 8) | (b & 0xff);
            bitCount += 8;
            while (bitCount >= 5 && index < 26) {
                buffer[index++] = ALPHABET.charAt((int) ((bits >>> (bitCount - 5)) & 31));
                bitCount -= 5;
            }
        }
        return new String(buffer);
    }
}
