package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.application.OrganisationDirectory;
import com.bank.workforce.bff.application.OrganisationDirectory.Branch;
import com.bank.workforce.bff.application.OrganisationDirectory.RelationshipManager;
import com.bank.workforce.bff.application.OrganisationDirectory.SpecifiedPerson;
import com.bank.workforce.bff.application.OrganisationDirectory.Vertical;
import com.bank.workforce.bff.client.PolicyDecisionClient;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OrganisationController {

  private final OrganisationDirectory directory;
  private final PolicyDecisionClient authorization;
  private final ServiceErrors errors;

  public OrganisationController(
      OrganisationDirectory directory, PolicyDecisionClient authorization, ServiceErrors errors) {
    this.directory = directory;
    this.authorization = authorization;
    this.errors = errors;
  }

  @GetMapping("/branches")
  public List<Branch> branches(HttpServletRequest request) {
    WorkforceSession session = BffSessionInterceptor.requireSession(request);
    authorization.require(session, "organisation.read", "organisation", null, null, false);
    return directory.branches();
  }

  @GetMapping("/branches/{branchId}/verticals")
  public List<Vertical> verticals(HttpServletRequest request, @PathVariable String branchId) {
    WorkforceSession session = BffSessionInterceptor.requireSession(request);
    authorization.require(session, "organisation.read", "organisation", branchId, branchId, false);
    requireBranch(branchId);
    return directory.verticalsForBranch(branchId);
  }

  @GetMapping("/branches/{branchId}/specified-persons")
  public List<SpecifiedPerson> specifiedPersons(
      HttpServletRequest request, @PathVariable String branchId) {
    WorkforceSession session = BffSessionInterceptor.requireSession(request);
    authorization.require(session, "organisation.read", "organisation", branchId, branchId, false);
    requireBranch(branchId);
    return directory.specifiedPersonsForBranch(branchId);
  }

  @GetMapping("/verticals/{verticalId}/relationship-managers")
  public List<RelationshipManager> relationshipManagers(
      HttpServletRequest request, @PathVariable String verticalId) {
    WorkforceSession session = BffSessionInterceptor.requireSession(request);
    authorization.require(session, "organisation.read", "organisation", verticalId, null, false);
    if (!directory.isKnownVertical(verticalId)) {
      throw errors
          .error(ErrorCodes.RESOURCE_NOT_FOUND)
          .component("OrganisationController")
          .operation("relationshipManagers")
          .reason("vertical is unknown")
          .build();
    }
    return directory.relationshipManagersForVertical(verticalId);
  }

  private void requireBranch(String branchId) {
    if (!directory.isKnownBranch(branchId)) {
      throw errors
          .error(ErrorCodes.RESOURCE_NOT_FOUND)
          .component("OrganisationController")
          .operation("branch")
          .reason("branch is unknown")
          .build();
    }
  }
}
