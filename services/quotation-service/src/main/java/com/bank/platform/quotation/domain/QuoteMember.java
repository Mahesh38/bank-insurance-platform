package com.bank.platform.quotation.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An insured member. PII: lives only inside the encrypted envelope (DB-DEC-0005). */
public record QuoteMember(
    String role,
    int sequence,
    LocalDate dob,
    String gender,
    boolean tobacco,
    BigDecimal annualIncome,
    String pincode) {}
