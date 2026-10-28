package com.bank.platform.integration.hub.domain;

/** Terminal failure on a provider request. */
public record Failure(String code, String reason) {}
