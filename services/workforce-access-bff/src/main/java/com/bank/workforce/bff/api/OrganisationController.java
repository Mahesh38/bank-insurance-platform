package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.application.OrganisationDirectory;
import com.bank.workforce.bff.application.OrganisationDirectory.Branch;
import com.bank.workforce.bff.application.OrganisationDirectory.RelationshipManager;
import com.bank.workforce.bff.application.OrganisationDirectory.SpecifiedPerson;
import com.bank.workforce.bff.application.OrganisationDirectory.Vertical;
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
  private final ServiceErrors errors;

  public OrganisationController(OrganisationDirectory directory, ServiceErrors errors) {
    this.directory = directory;
    this.errors = errors;
  }

  @GetMapping("/branches")
  public List<Branch> branches(HttpServletRequest request) {
    BffSessionInterceptor.requireSession(request);
    return directory.branches();
  }

  @GetMapping("/branches/{branchId}/verticals")
  public List<Vertical> verticals(HttpServletRequest request, @PathVariable String branchId) {
    BffSessionInterceptor.requireSession(request);
    requireBranch(branchId);
    return directory.verticalsForBranch(branchId);
  }

  @GetMapping("/branches/{branchId}/specified-persons")
  public List<SpecifiedPerson> specifiedPersons(
      HttpServletRequest request, @PathVariable String branchId) {
    BffSessionInterceptor.requireSession(request);
    requireBranch(branchId);
    return directory.specifiedPersonsForBranch(branchId);
  }

  @GetMapping("/verticals/{verticalId}/relationship-managers")
  public List<RelationshipManager> relationshipManagers(
      HttpServletRequest request, @PathVariable String verticalId) {
    BffSessionInterceptor.requireSession(request);
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
