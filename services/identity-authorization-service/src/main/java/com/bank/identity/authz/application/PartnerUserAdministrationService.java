package com.bank.identity.authz.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.identity.authz.domain.AuthorizationModels.Decision;
import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maker / checker administration of partner users.
 *
 * <p>Both actors are authenticated business users (the BFF asserts them from a session; see {@code
 * ActorPrincipalResolver}) and both are PDP-checked — {@code partner_user.create} for the maker,
 * {@code partner_user.approve} for the checker — scoped to the partner's insurer, so the tenant
 * boundary applies to an insurer's own administrator (READINESS-REVIEW §3.7).
 *
 * <p>Approval does <em>not</em> activate the partner: the user stays {@code PENDING} until the
 * provider identity is provisioned by the outbox ({@link ProvisioningOutboxProcessor}).
 */
@Service
public class PartnerUserAdministrationService {

  static final String CREATE_PERMISSION = "partner_user.create";
  static final String APPROVE_PERMISSION = "partner_user.approve";

  private final BusinessUserRepository users;
  private final JdbcClient jdbc;
  private final ServiceErrors errors;
  private final AuthorizationDecisionService decisions;

  public PartnerUserAdministrationService(
      BusinessUserRepository users,
      JdbcClient jdbc,
      ServiceErrors errors,
      AuthorizationDecisionService decisions) {
    this.users = users;
    this.jdbc = jdbc;
    this.errors = errors;
    this.decisions = decisions;
  }

  @Transactional
  public SubmittedPartnerUser submit(CreatePartnerUserCommand command, UUID makerId) {
    requirePermission(makerId, CREATE_PERMISSION, null, command.insurerCode(), "submit");
    if (users.existsByUsername(command.username())) {
      throw errors
          .error(ErrorCodes.CONFLICT)
          .component("PartnerUserAdministrationService")
          .operation("submit")
          .reason("Username already exists")
          .build();
    }
    BusinessUserEntity user =
        users.save(
            BusinessUserEntity.pendingPartner(
                command.username(),
                command.email(),
                command.firstName(),
                command.lastName(),
                command.insurerCode()));
    UUID approvalId = UUID.randomUUID();
    jdbc.sql(
            """
                insert into approval_request
                    (id, request_type, target_id, status, maker_id, requested_at)
                values (:id, 'PARTNER_USER_CREATE', :targetId, 'PENDING', :makerId, :now)
                """)
        .param("id", approvalId)
        .param("targetId", user.getId())
        .param("makerId", makerId.toString())
        .param("now", Instant.now())
        .update();
    return new SubmittedPartnerUser(user.getId(), approvalId, user.getStatus().name());
  }

  @Transactional
  public String approve(UUID approvalId, UUID checkerId) {
    Approval approval =
        jdbc.sql(
                """
                select target_id, maker_id, status
                  from approval_request
                 where id = :id
                 for update
                """)
            .param("id", approvalId)
            .query(
                (rs, rowNum) ->
                    new Approval(
                        rs.getObject("target_id", UUID.class),
                        rs.getString("maker_id"),
                        rs.getString("status")))
            .optional()
            .orElseThrow(
                () ->
                    errors
                        .error(ErrorCodes.RESOURCE_NOT_FOUND)
                        .component("PartnerUserAdministrationService")
                        .operation("approve")
                        .reason("Unknown approval request")
                        .build());
    if (!"PENDING".equals(approval.status())) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("PartnerUserAdministrationService")
          .operation("approve")
          .reason("Approval request is not pending")
          .build();
    }
    if (approval.makerId().equals(checkerId.toString())) {
      throw errors
          .error(ErrorCodes.FORBIDDEN)
          .component("PartnerUserAdministrationService")
          .operation("approve")
          .reason("Maker cannot approve their own request")
          .build();
    }
    BusinessUserEntity user =
        users
            .findById(approval.targetId())
            .orElseThrow(
                () ->
                    errors
                        .error(ErrorCodes.RESOURCE_NOT_FOUND)
                        .component("PartnerUserAdministrationService")
                        .operation("approve")
                        .reason("Approval target no longer exists")
                        .build());
    requirePermission(
        checkerId, APPROVE_PERMISSION, user.getId().toString(), user.getInsurerCode(), "approve");
    user.approve();
    jdbc.sql(
            """
                update approval_request
                   set status = 'APPROVED', checker_id = :checkerId, decided_at = :now
                 where id = :id
                """)
        .param("checkerId", checkerId.toString())
        .param("now", Instant.now())
        .param("id", approvalId)
        .update();
    jdbc.sql(
            """
                insert into outbox_event
                    (id, aggregate_type, aggregate_id, event_type, payload, created_at)
                values (:id, 'BUSINESS_USER', :aggregateId, 'IDENTITY_PROVISIONING_REQUESTED', :payload, :now)
                """)
        .param("id", UUID.randomUUID())
        .param("aggregateId", user.getId())
        .param("payload", "{\"businessUserId\":\"" + user.getId() + "\"}")
        .param("now", Instant.now())
        .update();
    return user.getStatus().name();
  }

  private void requirePermission(
      UUID actorId, String permission, String resourceId, String insurerCode, String operation) {
    Decision decision =
        decisions.decide(
            new DecisionRequest(
                actorId,
                permission,
                new Resource(
                    "partner_user", resourceId, null, insurerCode, null, Set.of(), true, false)));
    if (!decision.allowed()) {
      throw denial(decision.reasonCode(), operation, permission);
    }
  }

  private ServiceException denial(String reasonCode, String operation, String permission) {
    String code =
        switch (reasonCode == null ? "" : reasonCode) {
          case "ACCOUNT_NOT_ACTIVE" -> ErrorCodes.ACCOUNT_SUSPENDED;
          case "CROSS_INSURER_ACCESS" -> ErrorCodes.CROSS_INSURER_DENIED;
          case "EXPLICIT_DENY" -> ErrorCodes.EXPLICIT_DENY;
          default -> ErrorCodes.DEFAULT_DENY;
        };
    return errors
        .error(code)
        .component("PartnerUserAdministrationService")
        .operation(operation)
        .reason(permission + " denied: " + reasonCode)
        .build();
  }

  public record CreatePartnerUserCommand(
      String username, String email, String firstName, String lastName, String insurerCode) {}

  public record SubmittedPartnerUser(UUID userId, UUID approvalId, String status) {}

  private record Approval(UUID targetId, String makerId, String status) {}
}
