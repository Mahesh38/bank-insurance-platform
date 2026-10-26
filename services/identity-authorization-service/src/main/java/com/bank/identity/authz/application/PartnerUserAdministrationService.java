package com.bank.identity.authz.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.identity.authz.domain.AuthorizationModels.Decision;
import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration of partner (insurer representative) users.
 *
 * <p>DECISIONS-2026-10-22 E5: a bank admin holding {@code partner_user.create} creates the partner
 * user directly and provisioning into the identity provider is queued in the same transaction
 * (outbox {@code IDENTITY_PROVISIONING_REQUESTED}). The user stays {@code PENDING} until {@link
 * ProvisioningOutboxProcessor} has created the provider account, then becomes {@code ACTIVE}.
 *
 * <p>The retired maker-checker step is kept behind {@code
 * identity.partner-onboarding.approval-required} (default {@code false}). When it is on, creation
 * opens an {@code approval_request} instead and provisioning is queued only on approval by a
 * different actor holding {@code partner_user.approve}. When it is off, approve answers {@code 409
 * ILLEGAL_TRANSITION}.
 *
 * <p>Actors are authenticated business users (the BFF asserts them from a session; see {@code
 * ActorPrincipalResolver}) and every action is PDP-checked, scoped to the partner's insurer
 * (READINESS-REVIEW §3.7). The PDP never grants these actions to a partner representative.
 */
@Service
public class PartnerUserAdministrationService {

  static final String CREATE_PERMISSION = "partner_user.create";
  static final String APPROVE_PERMISSION = "partner_user.approve";
  static final String PROVISIONING_REQUESTED = "IDENTITY_PROVISIONING_REQUESTED";
  static final String PARTNER_USER_CREATED = "PARTNER_USER_CREATED";

  private final BusinessUserRepository users;
  private final JdbcClient jdbc;
  private final ServiceErrors errors;
  private final AuthorizationDecisionService decisions;
  private final ObjectMapper json;
  private final boolean approvalRequired;

  public PartnerUserAdministrationService(
      BusinessUserRepository users,
      JdbcClient jdbc,
      ServiceErrors errors,
      AuthorizationDecisionService decisions,
      ObjectMapper json,
      @Value("${identity.partner-onboarding.approval-required:false}") boolean approvalRequired) {
    this.users = users;
    this.jdbc = jdbc;
    this.errors = errors;
    this.decisions = decisions;
    this.json = json;
    this.approvalRequired = approvalRequired;
  }

  @Transactional
  public CreatedPartnerUser create(CreatePartnerUserCommand command, UUID creatorId) {
    requirePermission(creatorId, CREATE_PERMISSION, null, command.insurerCode(), "create");
    if (users.existsByUsername(command.username())) {
      throw errors
          .error(ErrorCodes.CONFLICT)
          .component("PartnerUserAdministrationService")
          .operation("create")
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
                command.insurerCode(),
                creatorId.toString()));
    UUID approvalId = null;
    if (approvalRequired) {
      approvalId = UUID.randomUUID();
      jdbc.sql(
              """
                  insert into approval_request
                      (id, request_type, target_id, status, maker_id, requested_at)
                  values (:id, 'PARTNER_USER_CREATE', :targetId, 'PENDING', :makerId, :now)
                  """)
          .param("id", approvalId)
          .param("targetId", user.getId())
          .param("makerId", creatorId.toString())
          .param("now", Timestamp.from(Instant.now()))
          .update();
    } else {
      enqueueProvisioning(user, creatorId);
    }
    // ADR-026 / E2: business audit travels as an outbox event (who created whom, and how).
    insertOutbox(
        user.getId(),
        PARTNER_USER_CREATED,
        payload(
            "businessUserId", user.getId(),
            "insurerCode", user.getInsurerCode(),
            "createdBy", creatorId,
            "approvalRequired", approvalRequired,
            "approvalId", approvalId));
    return new CreatedPartnerUser(
        user.getId(), approvalId, user.getStatus().name(), approvalRequired);
  }

  @Transactional
  public String approve(UUID approvalId, UUID checkerId) {
    if (!approvalRequired) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("PartnerUserAdministrationService")
          .operation("approve")
          .reason(
              "Partner-user approval is not required"
                  + " (identity.partner-onboarding.approval-required=false)")
          .build();
    }
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
        .param("now", Timestamp.from(Instant.now()))
        .param("id", approvalId)
        .update();
    enqueueProvisioning(user, checkerId);
    return user.getStatus().name();
  }

  private void enqueueProvisioning(BusinessUserEntity user, UUID requestedBy) {
    insertOutbox(
        user.getId(),
        PROVISIONING_REQUESTED,
        payload("businessUserId", user.getId(), "requestedBy", requestedBy));
  }

  private void insertOutbox(UUID aggregateId, String eventType, String payload) {
    jdbc.sql(
            """
                insert into outbox_event
                    (id, aggregate_type, aggregate_id, event_type, payload, created_at)
                values (:id, 'BUSINESS_USER', :aggregateId, :eventType, :payload, :now)
                """)
        .param("id", UUID.randomUUID())
        .param("aggregateId", aggregateId)
        .param("eventType", eventType)
        .param("payload", payload)
        .param("now", Timestamp.from(Instant.now()))
        .update();
  }

  private String payload(Object... keyValues) {
    Map<String, Object> body = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      Object value = keyValues[i + 1];
      body.put((String) keyValues[i], value instanceof UUID ? value.toString() : value);
    }
    try {
      return json.writeValueAsString(body);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Cannot serialise outbox payload", exception);
    }
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
          case "PARTNER_ACTION_PROHIBITED" -> ErrorCodes.FORBIDDEN;
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

  /** {@code approvalId} is null unless the maker-checker step is configured on. */
  public record CreatedPartnerUser(
      UUID userId, UUID approvalId, String status, boolean approvalRequired) {}

  private record Approval(UUID targetId, String makerId, String status) {}
}
