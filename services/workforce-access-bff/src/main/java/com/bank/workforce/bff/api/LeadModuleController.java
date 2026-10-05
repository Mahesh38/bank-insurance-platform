package com.bank.workforce.bff.api;

import com.bank.workforce.bff.application.LeadFacade;
import com.bank.workforce.bff.application.LeadFacade.ActiveLeadPage;
import com.bank.workforce.bff.lead.LeadGateway.CreateLeadCommand;
import com.bank.workforce.bff.lead.LeadGateway.LeadRecord;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LeadModuleController {

    private final LeadFacade leads;

    public LeadModuleController(LeadFacade leads) {
        this.leads = leads;
    }

    @GetMapping("/workspace/pipeline")
    public PipelinePage pipeline(HttpServletRequest request) {
        WorkforceSession session = BffSessionInterceptor.requireSession(request);
        List<LeadRecord> items = leads.pipeline(actorId(session));
        return new PipelinePage(items, null);
    }

    @PostMapping("/leads")
    public ResponseEntity<LeadRecord> create(
        HttpServletRequest request,
        @Valid @RequestBody CreateLeadRequest body
    ) {
        WorkforceSession session = BffSessionInterceptor.requireSession(request);
        LeadRecord created = leads.create(new CreateLeadCommand(
            body.customerId(), body.lob(), body.productClass(), body.branchId(), actorId(session)));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/leads/{leadId}")
    public LeadRecord get(HttpServletRequest request, @PathVariable String leadId) {
        WorkforceSession session = BffSessionInterceptor.requireSession(request);
        return leads.get(leadId, actorId(session));
    }

    @GetMapping("/customers/{customerId}/active-leads")
    public ActiveLeadPage activeLeads(
        HttpServletRequest request,
        @PathVariable String customerId,
        @RequestParam String productClass
    ) {
        WorkforceSession session = BffSessionInterceptor.requireSession(request);
        return leads.activeLeads(customerId, productClass, actorId(session));
    }

    @PostMapping("/leads/{leadId}/assignment")
    public LeadRecord assign(
        HttpServletRequest request,
        @PathVariable String leadId,
        @Valid @RequestBody AssignLeadRequest body
    ) {
        WorkforceSession session = BffSessionInterceptor.requireSession(request);
        return leads.assign(leadId, body.assignedRmId(), body.assignedSpId(), actorId(session));
    }

    private static String actorId(WorkforceSession session) {
        UUID id = session.businessUserId();
        return id != null ? id.toString() : session.username();
    }

    public record PipelinePage(List<LeadRecord> items, String nextCursor) {}

    public record CreateLeadRequest(
        @NotBlank String customerId,
        @NotBlank String lob,
        @NotBlank String productClass,
        String branchId
    ) {}

    public record AssignLeadRequest(
        @NotBlank String assignedRmId,
        String assignedSpId
    ) {}
}
