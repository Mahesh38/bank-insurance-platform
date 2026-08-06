package com.bank.identity.authz.application;

import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PartnerUserAdministrationService {

    private final BusinessUserRepository users;
    private final JdbcClient jdbc;

    public PartnerUserAdministrationService(BusinessUserRepository users, JdbcClient jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    @Transactional
    public SubmittedPartnerUser submit(CreatePartnerUserCommand command, String makerId) {
        if (users.existsByUsername(command.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        BusinessUserEntity user = users.save(BusinessUserEntity.pendingPartner(command.username(), command.email(),
            command.firstName(), command.lastName(), command.insurerCode()));
        UUID approvalId = UUID.randomUUID();
        jdbc.sql("""
                insert into approval_request
                    (id, request_type, target_id, status, maker_id, requested_at)
                values (:id, 'PARTNER_USER_CREATE', :targetId, 'PENDING', :makerId, :now)
                """)
            .param("id", approvalId)
            .param("targetId", user.getId())
            .param("makerId", makerId)
            .param("now", Instant.now())
            .update();
        return new SubmittedPartnerUser(user.getId(), approvalId, user.getStatus().name());
    }

    @Transactional
    public String approve(UUID approvalId, String checkerId) {
        Approval approval = jdbc.sql("""
                select target_id, maker_id, status
                  from approval_request
                 where id = :id
                 for update
                """)
            .param("id", approvalId)
            .query((rs, rowNum) -> new Approval(
                rs.getObject("target_id", UUID.class), rs.getString("maker_id"), rs.getString("status")))
            .optional()
            .orElseThrow(() -> new IllegalArgumentException("Unknown approval request"));
        if (!"PENDING".equals(approval.status())) {
            throw new IllegalStateException("Approval request is not pending");
        }
        if (approval.makerId().equals(checkerId)) {
            throw new IllegalArgumentException("Maker cannot approve their own request");
        }
        BusinessUserEntity user = users.findById(approval.targetId())
            .orElseThrow(() -> new IllegalStateException("Approval target no longer exists"));
        user.approve();
        jdbc.sql("""
                update approval_request
                   set status = 'APPROVED', checker_id = :checkerId, decided_at = :now
                 where id = :id
                """)
            .param("checkerId", checkerId)
            .param("now", Instant.now())
            .param("id", approvalId)
            .update();
        jdbc.sql("""
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

    public record CreatePartnerUserCommand(
        String username,
        String email,
        String firstName,
        String lastName,
        String insurerCode
    ) {}

    public record SubmittedPartnerUser(UUID userId, UUID approvalId, String status) {}

    private record Approval(UUID targetId, String makerId, String status) {}
}
