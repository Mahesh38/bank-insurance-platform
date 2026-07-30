package com.bank.common.security;

/**
 * JWT claim names used in bank-issued tokens.
 * Centralised here so all services extract the same claim keys.
 */
public final class JwtClaims {

    private JwtClaims() {}

    /** Employee ID of the RM (mapped to {@link BankPrincipal#getEmployeeId()}). */
    public static final String EMPLOYEE_ID  = "emp_id";

    /** Branch code where the employee operates. */
    public static final String BRANCH_CODE  = "branch_code";

    /**
     * Roles claim — value is a JSON array of {@link Role} names.
     * Example: {@code ["RM", "BRANCH_MANAGER"]}.
     */
    public static final String ROLES        = "roles";

    /**
     * Actor identifier — stable unique ID for audit events.
     * Typically same as {@link #EMPLOYEE_ID} for human actors.
     */
    public static final String ACTOR_ID     = "actor_id";

    /** Optional customer ID when the token represents a customer-facing flow. */
    public static final String CUSTOMER_ID  = "cust_id";
}
