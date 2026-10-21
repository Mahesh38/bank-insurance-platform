package com.bank.platform.lead.domain;

/**
 * Whether an employee holds a valid Specified Person certification.
 *
 * <p>IRDAI / bank SP register is the system of record. The fixture adapter is not that evidence
 * ({@code SUG-20261007-ird}).
 */
public interface SpCertificationPort {

  boolean holdsValidCertification(String empId);
}
