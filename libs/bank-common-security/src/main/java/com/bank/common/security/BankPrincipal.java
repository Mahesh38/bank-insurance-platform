package com.bank.common.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable representation of an authenticated bank actor (employee/RM).
 *
 * <p>Created by the JWT authentication filter from validated JWT claims. Service code should obtain
 * this from {@code SecurityContextHolder}.
 *
 * <p>PII note: never log the full principal object — {@link #toString()} emits only actorId to
 * prevent leaking customerId or roles into logs.
 */
public final class BankPrincipal {

  private final String actorId;

  private final String employeeId;
  private final String branchCode;
  private final Set<Role> roles;
  private final String customerId;

  public boolean hasRole(Role role) {
    return roles.contains(role);
  }

  public boolean hasAnyRole(Role... requiredRoles) {
    for (Role r : requiredRoles) {
      if (roles.contains(r)) {
        return true;
      }
    }
    return false;
  }

  public static class BankPrincipalBuilder {
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String actorId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String employeeId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String branchCode;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String customerId;

    private Set<Role> roles = EnumSet.noneOf(Role.class);

    public BankPrincipalBuilder roles(Set<Role> roles) {
      this.roles = EnumSet.copyOf(roles.isEmpty() ? EnumSet.noneOf(Role.class) : roles);
      return this;
    }

    public BankPrincipal build() {
      Set<Role> resolvedRoles =
          Collections.unmodifiableSet(
              roles == null || roles.isEmpty()
                  ? EnumSet.noneOf(Role.class)
                  : EnumSet.copyOf(roles));
      return new BankPrincipal(actorId, employeeId, branchCode, resolvedRoles, customerId);
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    BankPrincipalBuilder() {}

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public BankPrincipal.BankPrincipalBuilder actorId(final String actorId) {
      if (actorId == null) {
        throw new java.lang.NullPointerException("actorId is marked non-null but is null");
      }
      this.actorId = actorId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public BankPrincipal.BankPrincipalBuilder employeeId(final String employeeId) {
      if (employeeId == null) {
        throw new java.lang.NullPointerException("employeeId is marked non-null but is null");
      }
      this.employeeId = employeeId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public BankPrincipal.BankPrincipalBuilder branchCode(final String branchCode) {
      this.branchCode = branchCode;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public BankPrincipal.BankPrincipalBuilder customerId(final String customerId) {
      this.customerId = customerId;
      return this;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public java.lang.String toString() {
      return "BankPrincipal.BankPrincipalBuilder(actorId="
          + this.actorId
          + ", employeeId="
          + this.employeeId
          + ", branchCode="
          + this.branchCode
          + ", roles="
          + this.roles
          + ", customerId="
          + this.customerId
          + ")";
    }
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  BankPrincipal(
      final String actorId,
      final String employeeId,
      final String branchCode,
      final Set<Role> roles,
      final String customerId) {
    if (actorId == null) {
      throw new java.lang.NullPointerException("actorId is marked non-null but is null");
    }
    if (employeeId == null) {
      throw new java.lang.NullPointerException("employeeId is marked non-null but is null");
    }
    this.actorId = actorId;
    this.employeeId = employeeId;
    this.branchCode = branchCode;
    this.roles = roles;
    this.customerId = customerId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public static BankPrincipal.BankPrincipalBuilder builder() {
    return new BankPrincipal.BankPrincipalBuilder();
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getActorId() {
    return this.actorId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getEmployeeId() {
    return this.employeeId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getBranchCode() {
    return this.branchCode;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public Set<Role> getRoles() {
    return this.roles;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getCustomerId() {
    return this.customerId;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public boolean equals(final java.lang.Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof BankPrincipal)) {
      return false;
    }
    final BankPrincipal other = (BankPrincipal) o;
    final java.lang.Object this$actorId = this.getActorId();
    final java.lang.Object other$actorId = other.getActorId();
    if (this$actorId == null ? other$actorId != null : !this$actorId.equals(other$actorId)) {
      return false;
    }
    final java.lang.Object this$employeeId = this.getEmployeeId();
    final java.lang.Object other$employeeId = other.getEmployeeId();
    if (this$employeeId == null
        ? other$employeeId != null
        : !this$employeeId.equals(other$employeeId)) {
      return false;
    }
    return true;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final java.lang.Object $actorId = this.getActorId();
    result = result * PRIME + ($actorId == null ? 43 : $actorId.hashCode());
    final java.lang.Object $employeeId = this.getEmployeeId();
    result = result * PRIME + ($employeeId == null ? 43 : $employeeId.hashCode());
    return result;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public java.lang.String toString() {
    return "BankPrincipal(actorId=" + this.getActorId() + ")";
  }
}
