package com.bank.identity.authz.persistence;

import com.bank.identity.authz.domain.IdentityTypes.UserStatus;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_user")
public class BusinessUserEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_subject")
    private String providerSubject;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private String username;

    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "insurer_code")
    private String insurerCode;

    @Column(name = "policy_version", nullable = false)
    private long policyVersion;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected BusinessUserEntity() {
    }

    public static BusinessUserEntity bankEmployee(
        String provider,
        String providerSubject,
        String username,
        String email,
        String employeeId
    ) {
        Instant now = Instant.now();
        var user = new BusinessUserEntity();
        user.id = UUID.randomUUID();
        user.provider = provider;
        user.providerSubject = providerSubject;
        user.userType = UserType.BANK_EMPLOYEE;
        user.status = UserStatus.ACTIVE;
        user.username = username;
        user.email = email;
        user.employeeId = employeeId;
        user.policyVersion = 1;
        user.validFrom = now;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public static BusinessUserEntity pendingPartner(
        String username,
        String email,
        String firstName,
        String lastName,
        String insurerCode
    ) {
        Instant now = Instant.now();
        var user = new BusinessUserEntity();
        user.id = UUID.randomUUID();
        user.provider = "KEYCLOAK";
        user.userType = UserType.INSURER_REPRESENTATIVE;
        user.status = UserStatus.PENDING;
        user.username = username;
        user.email = email;
        user.firstName = firstName;
        user.lastName = lastName;
        user.insurerCode = insurerCode;
        user.policyVersion = 1;
        user.validFrom = now;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    public void refreshProfile(String username, String email, String employeeId) {
        this.username = username;
        this.email = email;
        this.employeeId = employeeId;
        this.updatedAt = Instant.now();
    }

    public void approve() {
        this.policyVersion++;
        this.updatedAt = Instant.now();
    }

    public void markProvisioned(String providerSubject) {
        this.providerSubject = providerSubject;
        this.status = UserStatus.ACTIVE;
        this.policyVersion++;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getProvider() { return provider; }
    public String getProviderSubject() { return providerSubject; }
    public UserType getUserType() { return userType; }
    public UserStatus getStatus() { return status; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmployeeId() { return employeeId; }
    public String getInsurerCode() { return insurerCode; }
    public long getPolicyVersion() { return policyVersion; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
}
