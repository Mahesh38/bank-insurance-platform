package com.bank.platform.quotation.domain;

/** A per-insurer failure shown beside the offers of a {@code PARTIALLY_QUOTED} quote. */
public record InsurerError(String insurerCode, String code) {}
