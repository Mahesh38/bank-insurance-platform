package com.bank.platform.proposal.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The nominee. Always inside the encrypted envelope, never a control attribute (INV-PRP-05). */
public record Nominee(
    String name, LocalDate dateOfBirth, String relationship, BigDecimal sharePercent) {}
