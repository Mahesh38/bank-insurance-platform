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

  @Id private UUID id;

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

  @Column(name = "created_by")
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "first_login", nullable = false)
  private boolean firstLogin;

  @Column(name = "failed_password_count", nullable = false)
  private int failedPasswordCount;

  @Column(name = "locked_reason")
  private String lockedReason;

  @Column(name = "locked_at")
  private Instant lockedAt;

  /** When a lock was cleared. Inactivity is measured from the later of this and the last login. */
  @Column(name = "lock_cleared_at")
  private Instant lockClearedAt;

  @Column(name = "last_successful_login_at")
  private Instant lastSuccessfulLoginAt;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "password_changed_at")
  private Instant passwordChangedAt;

  @Column(name = "mobile_number")
  private String mobileNumber;

  @Version private long version;

  protected BusinessUserEntity() {}

  public static BusinessUserEntity bankEmployee(
      String provider, String providerSubject, String username, String email, String employeeId) {
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
      String username, String email, String firstName, String lastName, String insurerCode) {
    return pendingPartner(username, email, firstName, lastName, insurerCode, null);
  }

  /** E5: a partner created directly by a bank admin; {@code createdBy} is that admin's id. */
  public static BusinessUserEntity pendingPartner(
      String username,
      String email,
      String firstName,
      String lastName,
      String insurerCode,
      String createdBy) {
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
    user.createdBy = createdBy;
    user.policyVersion = 1;
    user.validFrom = now;
    user.createdAt = now;
    user.updatedAt = now;
    user.firstLogin = true;
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

  public UUID getId() {
    return id;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderSubject() {
    return providerSubject;
  }

  public UserType getUserType() {
    return userType;
  }

  public UserStatus getStatus() {
    return status;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmployeeId() {
    return employeeId;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public long getPolicyVersion() {
    return policyVersion;
  }

  public Instant getValidFrom() {
    return validFrom;
  }

  public Instant getValidUntil() {
    return validUntil;
  }

  public boolean isFirstLogin() {
    return firstLogin;
  }

  public int getFailedPasswordCount() {
    return failedPasswordCount;
  }

  public String getLockedReason() {
    return lockedReason;
  }

  public Instant getLastSuccessfulLoginAt() {
    return lastSuccessfulLoginAt;
  }

  public Instant getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getMobileNumber() {
    return mobileNumber;
  }

  public void assignMobileNumber(String mobileNumber) {
    this.mobileNumber = mobileNumber;
    this.updatedAt = Instant.now();
  }

  public void assignName(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.updatedAt = Instant.now();
  }

  public void assignProviderSubject(String providerSubject) {
    this.providerSubject = providerSubject;
    this.updatedAt = Instant.now();
  }

  public void replacePasswordHash(String passwordHash, Instant changedAt) {
    this.passwordHash = passwordHash;
    this.passwordChangedAt = changedAt;
    this.firstLogin = false;
    if (this.userType == UserType.INSURER_REPRESENTATIVE && this.status == UserStatus.PENDING) {
      this.status = UserStatus.ACTIVE;
    }
    this.updatedAt = changedAt;
  }

  /** v3.1 first-time Bank RM row. AD resolve does not set this; provisioning does. */
  public void markFirstLoginForProvisioning() {
    this.firstLogin = true;
    this.updatedAt = Instant.now();
  }

  public void suspend() {
    this.status = UserStatus.SUSPENDED;
    this.updatedAt = Instant.now();
  }

  public boolean recordPasswordFailure(int maxFailures, Instant now) {
    this.failedPasswordCount++;
    this.updatedAt = now;
    if (this.failedPasswordCount >= maxFailures) {
      this.lockedReason = "PASSWORD";
      this.lockedAt = now;
      return true;
    }
    return false;
  }

  public void lockForInactivity(Instant now) {
    this.lockedReason = "INACTIVITY";
    this.lockedAt = now;
    this.updatedAt = now;
  }

  public void clearLockAndFailures(Instant now) {
    this.lockedReason = null;
    this.lockedAt = null;
    this.lockClearedAt = now;
    this.failedPasswordCount = 0;
    this.updatedAt = now;
  }

  public Instant getLockClearedAt() {
    return lockClearedAt;
  }

  public void recordSuccessfulPlatformLogin(Instant now) {
    this.failedPasswordCount = 0;
    this.lockedReason = null;
    this.lockedAt = null;
    this.lastSuccessfulLoginAt = now;
    this.updatedAt = now;
  }

  public void activateBankFirstLogin(Instant now) {
    this.firstLogin = false;
    this.status = UserStatus.ACTIVE;
    clearLockAndFailures(now);
  }

  public void openInsurancePasswordSetup(Instant now) {
    this.status = UserStatus.ACTIVE;
    clearLockAndFailures(now);
    this.firstLogin = true;
  }
}
