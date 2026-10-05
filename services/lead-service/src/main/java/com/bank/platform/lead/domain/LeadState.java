package com.bank.platform.lead.domain;

/**
 * Lead aggregate machine ({@code 01-domain-model} §4.1). Dashboard labels from
 * Lead BRD §14.1 are a projection — see {@link DashboardStage} and {@code D-021}.
 */
public enum LeadState {
    NEW,
    ASSIGNED,
    CONTACTED,
    QUALIFIED,
    CONVERTED,
    DISQUALIFIED,
    EXPIRED,
    ARCHIVED
}
