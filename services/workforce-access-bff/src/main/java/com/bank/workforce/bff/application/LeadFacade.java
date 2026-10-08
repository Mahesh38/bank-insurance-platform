package com.bank.workforce.bff.application;

import com.bank.workforce.bff.lead.LeadGateway;
import com.bank.workforce.bff.lead.LeadGateway.AssignLeadCommand;
import com.bank.workforce.bff.lead.LeadGateway.CreateLeadCommand;
import com.bank.workforce.bff.lead.LeadGateway.LeadRecord;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * BFF orchestration for the Lead module. Assign evaluates exception rules first ({@code D-019}) and
 * returns whether an exception still needs to be taken.
 */
@Service
public class LeadFacade {

  private final LeadGateway leads;

  public LeadFacade(LeadGateway leads) {
    this.leads = leads;
  }

  public LeadRecord create(CreateLeadCommand command) {
    return leads.create(command);
  }

  public LeadRecord get(String leadId, String actorId) {
    return leads.get(leadId, actorId);
  }

  public List<LeadRecord> pipeline(String actorId) {
    return leads.list(actorId);
  }

  public ActiveLeadPage activeLeads(String customerId, String productClass, String actorId) {
    List<LeadRecord> matches =
        leads.list(actorId).stream()
            .filter(lead -> customerId.equals(lead.customerId()))
            .filter(lead -> productClass == null || productClass.equals(lead.productClass()))
            .filter(LeadFacade::active)
            .toList();
    boolean hasMore = matches.size() > 20;
    List<LeadRecord> items = hasMore ? matches.subList(0, 20) : matches;
    return new ActiveLeadPage(items, hasMore);
  }

  public LeadRecord assign(
      String leadId, String assignedRmId, String assignedSpId, String actorId) {
    LeadRecord current = leads.get(leadId, actorId);
    if ("NOT_EVALUATED".equals(current.exceptionOutcome())) {
      current = leads.startOnboarding(leadId, actorId);
    }
    if (current.exceptionRequired()
        || current.exceptionHold()
        || "APPROVAL_REQUIRED".equals(current.exceptionOutcome())
        || "BLOCK".equals(current.exceptionOutcome())) {
      return current;
    }
    return leads.assign(new AssignLeadCommand(leadId, assignedRmId, assignedSpId, actorId));
  }

  private static boolean active(LeadRecord lead) {
    String state = lead.state();
    return !"CONVERTED".equals(state)
        && !"DISQUALIFIED".equals(state)
        && !"EXPIRED".equals(state)
        && !"ARCHIVED".equals(state);
  }

  public record ActiveLeadPage(List<LeadRecord> items, boolean hasMore) {}
}
