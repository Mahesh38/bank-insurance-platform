package com.bank.platform.integration.hub.application;

/** 202 Accepted body for provider-request submit. */
public record ProviderRequestAccepted(
    String providerRequestId,
    String operation,
    String state,
    String provider,
    String routingVersion) {}
